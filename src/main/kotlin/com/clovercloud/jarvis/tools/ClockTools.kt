package com.clovercloud.jarvis.tools

import com.clovercloud.jarvis.responses.ClockResponse
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class ClockTools {

    @McpTool(
        name = "get_current_time",
        description = "Returns current time, date, day of week, and epoch timestamp for a timezone."
    )
    fun getCurrentTime(
        @McpToolParam(description = "Timezone ID (e.g. UTC, Europe/Amsterdam, America/New_York)", required = false)
        timezone: String?
    ): ClockResponse {
        val tz = timezone?.takeIf { it.isNotBlank() } ?: "UTC"
        val zoneId = try {
            ZoneId.of(tz)
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }
        val now = ZonedDateTime.now(zoneId)
        return ClockResponse(
            isoTimestamp = now.toOffsetDateTime().toString(),
            formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            dayOfWeek = now.dayOfWeek.name,
            timeZone = zoneId.id,
            epochSeconds = Instant.now().epochSecond
        )
    }
}
