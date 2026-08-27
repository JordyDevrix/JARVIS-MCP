package com.clovercloud.jarvis.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.clovercloud.jarvis.responses.ClockResponse

@RestController
@RequestMapping("/v1/tools/clock")
@Tag(name = "Clock Tool", description = "Provides real-time clock and timezone information for Jarvis")
class ClockController {

    @Operation(summary = "Get current time", description = "Returns current time, date, day of week, and epoch timestamp for the specified timezone.")
    @GetMapping("/time")
    fun getCurrentTime(
        @Parameter(description = "Timezone ID (e.g., Europe/Amsterdam, UTC, America/New_York)", example = "Europe/Amsterdam")
        @RequestParam(required = false, defaultValue = "Europe/Amsterdam") timezone: String
    ): ResponseEntity<ClockResponse> {
        val zoneId = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            ZoneId.of("Europe/Amsterdam")
        }

        val now = ZonedDateTime.now(zoneId)
        val formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val response = ClockResponse(
            isoTimestamp = now.toOffsetDateTime().toString(),
            formattedTime = formattedTime,
            formattedDate = formattedDate,
            dayOfWeek = now.dayOfWeek.name,
            timeZone = zoneId.id,
            epochSeconds = Instant.now().epochSecond,
            dateTime24h = "$formattedDate $formattedTime",
            note = "Instantaneous current time snapshot for ${zoneId.id}. Valid for the current second."
        )

        return ResponseEntity.ok(response)
    }
}
