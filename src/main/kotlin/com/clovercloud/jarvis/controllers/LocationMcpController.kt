package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.requests.LocationRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/mcp/location")
@Tag(name = "Location MCP", description = "MCP-compliant endpoint for location tool")
class LocationMcpController {
    private val presetLocations = mapOf(
        "amsterdam" to LocationResponse("Amsterdam", "Netherlands", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "Mock GPS location for Amsterdam headquarters"),
        "new york" to LocationResponse("New York", "United States", "US", 40.7128, -74.0060, "America/New_York", "Mock GPS location for New York branch"),
        "london" to LocationResponse("London", "United Kingdom", "GB", 51.5074, -0.1278, "Europe/London", "Mock GPS location for London office"),
        "tokyo" to LocationResponse("Tokyo", "Japan", "JP", 35.6762, 139.6503, "Asia/Tokyo", "Mock GPS location for Tokyo center")
    )

    @Operation(
        summary = "Execute Location MCP tool",
        description = "Returns location data for a city in an MCP-compliant result."
    )
    @PostMapping
    fun executeLocation(@RequestBody body: LocationRequest?): ResponseEntity<LocationResponse> {
        val city = body?.city ?: "Amsterdam"
        val normalized = city.trim().lowercase()
        val location = presetLocations[normalized] ?: LocationResponse(
            city = city.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            country = "Unknown",
            countryCode = "N/A",
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            description = "Simulated location for $city"
        )
        return ResponseEntity.ok(location)
    }
}
