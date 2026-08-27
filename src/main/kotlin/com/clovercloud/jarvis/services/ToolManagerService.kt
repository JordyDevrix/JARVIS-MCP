package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.config.McpToolsProperties
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.server.McpSyncServer
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

data class ToolInfo(
    val name: String,
    val description: String?,
    val enabled: Boolean
)

@Service
class ToolManagerService(
    @Lazy private val mcpSyncServer: McpSyncServer,
    private val applicationContext: ApplicationContext,
    private val properties: McpToolsProperties
) {
    private val logger = LoggerFactory.getLogger(ToolManagerService::class.java)

    private val toolRegistry = ConcurrentHashMap<String, McpServerFeatures.SyncToolSpecification>()
    private val toolStatus = ConcurrentHashMap<String, Boolean>()

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        val toolObjects = applicationContext.getBeansWithAnnotation(Component::class.java).values.filter { bean ->
            bean.javaClass.methods.any { it.isAnnotationPresent(McpTool::class.java) }
        }
        val specs = SyncMcpAnnotationProviders.toolSpecifications(toolObjects.toList())
        var disabledCount = 0

        for (spec in specs) {
            val name = spec.tool().name()
            toolRegistry[name] = spec

            val shouldEnable = properties.isEnabledOnStartup(name)
            toolStatus[name] = shouldEnable

            if (!shouldEnable) {
                try {
                    mcpSyncServer.removeTool(name)
                    disabledCount++
                    logger.info("MCP tool '$name' is DISABLED on startup via configuration")
                } catch (e: Exception) {
                    logger.warn("Failed to unregister disabled tool '$name' on startup: ${e.message}")
                }
            }
        }

        if (disabledCount > 0) {
            mcpSyncServer.notifyToolsListChanged()
        }

        val activeCount = toolStatus.values.count { it }
        logger.info("ToolManagerService initialized with ${toolRegistry.size} tools ($activeCount enabled, $disabledCount disabled on startup)")
    }

    fun listTools(): List<ToolInfo> {
        return toolRegistry.map { (name, spec) ->
            ToolInfo(
                name = name,
                description = spec.tool().description(),
                enabled = toolStatus[name] ?: true
            )
        }.sortedBy { it.name }
    }

    fun isToolEnabled(name: String): Boolean = toolStatus[name] ?: false

    @Synchronized
    fun toggleTool(name: String): ToolInfo? {
        val currentlyEnabled = toolStatus[name] ?: return null
        return if (currentlyEnabled) {
            disableTool(name)
        } else {
            enableTool(name)
        }
    }

    @Synchronized
    fun enableTool(name: String): ToolInfo? {
        val spec = toolRegistry[name] ?: return null
        if (toolStatus[name] != true) {
            try {
                mcpSyncServer.addTool(spec)
                mcpSyncServer.notifyToolsListChanged()
                toolStatus[name] = true
                logger.info("MCP tool '$name' has been ENABLED")
            } catch (e: Exception) {
                logger.error("Failed to enable tool '$name': ${e.message}", e)
                throw e
            }
        }
        return ToolInfo(name, spec.tool().description(), true)
    }

    @Synchronized
    fun disableTool(name: String): ToolInfo? {
        val spec = toolRegistry[name] ?: return null
        if (toolStatus[name] != false) {
            try {
                mcpSyncServer.removeTool(name)
                mcpSyncServer.notifyToolsListChanged()
                toolStatus[name] = false
                logger.info("MCP tool '$name' has been DISABLED")
            } catch (e: Exception) {
                logger.error("Failed to disable tool '$name': ${e.message}", e)
                throw e
            }
        }
        return ToolInfo(name, spec.tool().description(), false)
    }
}
