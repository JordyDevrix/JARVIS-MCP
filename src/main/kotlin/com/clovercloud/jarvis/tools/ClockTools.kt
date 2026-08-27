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
        description = "Returns current time, date, day of week, and epoch timestamp for a timezone (defaults to Europe/Amsterdam)."
    )
    fun getCurrentTime(
        @McpToolParam(description = "Timezone ID (e.g. Europe/Amsterdam, UTC, America/New_York). Defaults to Europe/Amsterdam.", required = false)
        timezone: String?
    ): ClockResponse {
        val tz = timezone?.takeIf { it.isNotBlank() } ?: "Europe/Amsterdam"
        val zoneId = try {
            ZoneId.of(tz)
        } catch (e: Exception) {
            ZoneId.of("Europe/Amsterdam")
        }
        val now = ZonedDateTime.now(zoneId)
        val formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return ClockResponse(
            isoTimestamp = now.toOffsetDateTime().toString(),
            formattedTime = formattedTime,
            formattedDate = formattedDate,
            dayOfWeek = now.dayOfWeek.name,
            timeZone = zoneId.id,
            epochSeconds = Instant.now().epochSecond,
            dateTime24h = "$formattedDate $formattedTime",
            note = "Instantaneous current time snapshot for ${zoneId.id}. Valid for the current second."
        )
    }
}
