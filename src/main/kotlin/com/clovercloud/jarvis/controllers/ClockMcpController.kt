package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.responses.ClockResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Defines the expected MCP request body for clock

data class ClockRequest(val timezone: String? = "UTC")

@RestController
@RequestMapping("/v1/mcp/clock")
@Tag(name = "Clock MCP", description = "MCP-compliant endpoint for clock tool")
class ClockMcpController {
    @Operation(
        summary = "Execute Clock MCP tool",
        description = "Returns current time, date, day of week, and epoch timestamp for a timezone as an MCP-compliant result."
    )
    @PostMapping
    fun executeClock(@RequestBody body: ClockRequest?): ResponseEntity<ClockResponse> {
        val timezone = body?.timezone ?: "UTC"
        val zoneId = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }
        val now = ZonedDateTime.now(zoneId)
        val response = ClockResponse(
            isoTimestamp = now.toOffsetDateTime().toString(),
            formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            dayOfWeek = now.dayOfWeek.name,
            timeZone = zoneId.id,
            epochSeconds = Instant.now().epochSecond
        )
        return ResponseEntity.ok(response)
    }
}
