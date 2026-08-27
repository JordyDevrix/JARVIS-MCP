package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.services.ToolInfo
import com.clovercloud.jarvis.services.ToolManagerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/tools/management")
@Tag(name = "Tool Management", description = "Endpoints for inspecting and toggling MCP tools at runtime without restarting")
class ToolManagementController(
    @Lazy private val toolManagerService: ToolManagerService
) {

    @Operation(summary = "List all MCP tools and their current enabled status")
    @ApiResponse(responseCode = "200", description = "Returns the list of all tools with status")
    @GetMapping
    fun listTools(): ResponseEntity<List<ToolInfo>> =
        ResponseEntity.ok(toolManagerService.listTools())

    @Operation(summary = "Toggle an MCP tool's enabled/disabled status")
    @ApiResponse(responseCode = "200", description = "Tool successfully toggled")
    @ApiResponse(responseCode = "404", description = "Tool name not found")
    @PostMapping("/{toolName}/toggle")
    fun toggleTool(@PathVariable toolName: String): ResponseEntity<Any> {
        val result = toolManagerService.toggleTool(toolName)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Tool '$toolName' not found"))
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "Enable an MCP tool")
    @ApiResponse(responseCode = "200", description = "Tool successfully enabled")
    @ApiResponse(responseCode = "404", description = "Tool name not found")
    @PostMapping("/{toolName}/enable")
    fun enableTool(@PathVariable toolName: String): ResponseEntity<Any> {
        val result = toolManagerService.enableTool(toolName)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Tool '$toolName' not found"))
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "Disable an MCP tool")
    @ApiResponse(responseCode = "200", description = "Tool successfully disabled")
    @ApiResponse(responseCode = "404", description = "Tool name not found")
    @PostMapping("/{toolName}/disable")
    fun disableTool(@PathVariable toolName: String): ResponseEntity<Any> {
        val result = toolManagerService.disableTool(toolName)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Tool '$toolName' not found"))
        return ResponseEntity.ok(result)
    }
}
