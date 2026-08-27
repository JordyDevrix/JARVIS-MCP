package com.clovercloud.jarvis.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class LocationResponse(
    val city: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val description: String
)

@RestController
@RequestMapping("/v1/tools/location")
@Tag(name = "Location Tool", description = "Mock location service for Jarvis")
class LocationController {

    private val presetLocations = mapOf(
        "amsterdam" to LocationResponse("Amsterdam", "Netherlands", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "Mock GPS location for Amsterdam headquarters"),
        "new york" to LocationResponse("New York", "United States", "US", 40.7128, -74.0060, "America/New_York", "Mock GPS location for New York branch"),
        "london" to LocationResponse("London", "United Kingdom", "GB", 51.5074, -0.1278, "Europe/London", "Mock GPS location for London office"),
        "tokyo" to LocationResponse("Tokyo", "Japan", "JP", 35.6762, 139.6503, "Asia/Tokyo", "Mock GPS location for Tokyo center")
    )

    @Operation(summary = "Get current location", description = "Returns mock current location of the device/system.")
    @GetMapping("/current")
    fun getCurrentLocation(): ResponseEntity<LocationResponse> {
        val defaultLocation = presetLocations["amsterdam"]!!
        return ResponseEntity.ok(defaultLocation)
    }

    @Operation(summary = "Lookup location by city name", description = "Returns coordinates and location info for a city.")
    @GetMapping("/lookup")
    fun lookupLocation(
        @Parameter(description = "City name to lookup", example = "Amsterdam")
        @RequestParam(required = false, defaultValue = "Amsterdam") city: String
    ): ResponseEntity<LocationResponse> {
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
