package com.clovercloud.jarvis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "mcptools")
class McpToolsProperties {
    /**
     * Map of tool name to boolean flag (true = enabled, false = disabled).
     * Supports exact name, snake_case, or kebab-case.
     */
    var enabled: MutableMap<String, Boolean> = mutableMapOf()

    /**
     * List of tool names that should be disabled at startup.
     */
    var disabled: MutableList<String> = mutableListOf()

    fun isEnabledOnStartup(toolName: String): Boolean {
        val norm = normalize(toolName)

        // Check explicit disabled list first
        if (disabled.any { normalize(it) == norm }) {
            return false
        }

        // Check enabled map
        val entry = enabled.entries.firstOrNull { normalize(it.key) == norm }
        if (entry != null) {
            return entry.value
        }

        // Default to true if not specified
        return true
    }

    private fun normalize(name: String): String =
        name.lowercase().replace("_", "").replace("-", "")
}
