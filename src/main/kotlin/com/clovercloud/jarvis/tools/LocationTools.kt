package com.clovercloud.jarvis.tools

import com.clovercloud.jarvis.responses.LocationResponse
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class LocationTools {
    private val presetLocations = mapOf(
        "amsterdam" to LocationResponse("Amsterdam", "Netherlands", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "Mock GPS location for Amsterdam headquarters"),
        "new york" to LocationResponse("New York", "United States", "US", 40.7128, -74.0060, "America/New_York", "Mock GPS location for New York branch"),
        "london" to LocationResponse("London", "United Kingdom", "GB", 51.5074, -0.1278, "Europe/London", "Mock GPS location for London office"),
        "tokyo" to LocationResponse("Tokyo", "Japan", "JP", 35.6762, 139.6503, "Asia/Tokyo", "Mock GPS location for Tokyo center")
    )

    @McpTool(
        name = "get_current_location",
        description = "Returns mock current GPS location of the system/device (Amsterdam headquarters)."
    )
    fun getCurrentLocation(): LocationResponse {
        return presetLocations["amsterdam"]!!
    }

    @McpTool(
        name = "lookup_location",
        description = "Lookup coordinates and location info for a city."
    )
    fun lookupLocation(
        @McpToolParam(description = "City name to lookup (e.g., Amsterdam, New York, London, Tokyo)", required = false)
        city: String?
    ): LocationResponse {
        val cityName = city?.takeIf { it.isNotBlank() } ?: "Amsterdam"
        val normalized = cityName.trim().lowercase()
        return presetLocations[normalized] ?: LocationResponse(
            city = cityName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            country = "Unknown",
            countryCode = "N/A",
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            description = "Simulated location for $cityName"
        )
    }
}
