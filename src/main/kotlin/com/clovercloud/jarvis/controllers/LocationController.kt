package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.facades.LocationFacade
import com.clovercloud.jarvis.requests.LocationRequest
import com.clovercloud.jarvis.requests.location.CurrentLocationRequest
import com.clovercloud.jarvis.requests.location.GeocodeRequest
import com.clovercloud.jarvis.requests.location.NearbyPlacesRequest
import com.clovercloud.jarvis.requests.location.ReverseGeocodeRequest
import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.responses.location.GeocodeResponse
import com.clovercloud.jarvis.responses.location.NearbyPlacesResponse
import com.clovercloud.jarvis.responses.location.ReverseGeocodeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
@Tag(name = "Location Tools", description = "Real-time geographic location, reverse geocoding, address search, and nearby amenities via OpenStreetMap")
class LocationController(
    private val locationFacade: LocationFacade
) {

    @Operation(summary = "Get current location", description = "Determines real current location using IP geolocation with fallback.")
    @GetMapping("/v1/tools/location/current")
    fun getCurrentLocation(): ResponseEntity<LocationResponse> {
        val location = locationFacade.getCurrentLocation()
        return ResponseEntity.ok(location)
    }

    @Operation(summary = "Reverse geocode coordinates", description = "Converts latitude and longitude into human-readable address and place details using OpenStreetMap.")
    @GetMapping("/v1/tools/location/reverse")
    fun reverseGeocode(
        @Parameter(description = "Latitude coordinate", example = "52.3676", required = true)
        @RequestParam latitude: Double,
        @Parameter(description = "Longitude coordinate", example = "4.9041", required = true)
        @RequestParam longitude: Double,
        @Parameter(description = "Detail zoom level (3=country, 10=city, 18=building)", example = "18")
        @RequestParam(required = false, defaultValue = "18") zoom: Int
    ): ResponseEntity<ReverseGeocodeResponse> {
        val response = locationFacade.reverseGeocode(latitude, longitude, zoom)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "Geocode address / search location", description = "Searches for an address, landmark, or city worldwide using OpenStreetMap.")
    @GetMapping("/v1/tools/location/geocode")
    fun geocode(
        @Parameter(description = "Search query for place or address", example = "Dam Square Amsterdam", required = true)
        @RequestParam query: String,
        @Parameter(description = "Optional two-letter country code filter", example = "nl")
        @RequestParam(required = false) country_code: String?,
        @Parameter(description = "Maximum results", example = "5")
        @RequestParam(required = false, defaultValue = "5") limit: Int
    ): ResponseEntity<GeocodeResponse> {
        val response = locationFacade.geocode(query, country_code, limit)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "Lookup location by city name (legacy compatible)", description = "Returns coordinates and location info for a city.")
    @GetMapping("/v1/tools/location/lookup")
    fun lookupLocation(
        @Parameter(description = "City name to lookup", example = "Amsterdam")
        @RequestParam(required = false, defaultValue = "Amsterdam") city: String
    ): ResponseEntity<LocationResponse> {
        val geo = locationFacade.geocode(city, limit = 1)
        val first = geo.places.firstOrNull()
        val location = if (first != null) {
            LocationResponse(
                city = first.address?.city ?: first.address?.town ?: city,
                country = first.address?.country ?: "Unknown",
                countryCode = first.address?.countryCode ?: "N/A",
                latitude = first.latitude,
                longitude = first.longitude,
                timezone = "UTC",
                description = first.displayName,
                region = first.address?.state,
                postalCode = first.address?.postcode,
                source = "OPENSTREETMAP"
            )
        } else {
            locationFacade.getCurrentLocation()
        }
        return ResponseEntity.ok(location)
    }

    @Operation(summary = "Find nearby places / amenities", description = "Finds points of interest and amenities around coordinates with distance and bearing.")
    @GetMapping("/v1/tools/location/nearby")
    fun findNearby(
        @Parameter(description = "Amenity or category (pharmacy, hospital, supermarket, restaurant, parking, atm, charging_station)", example = "pharmacy", required = true)
        @RequestParam query: String,
        @Parameter(description = "Observer latitude (defaults to current location)")
        @RequestParam(required = false) latitude: Double?,
        @Parameter(description = "Observer longitude (defaults to current location)")
        @RequestParam(required = false) longitude: Double?,
        @Parameter(description = "Search radius in km", example = "1.0")
        @RequestParam(required = false, defaultValue = "1.0") radius_km: Double,
        @Parameter(description = "Maximum places to return", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int
    ): ResponseEntity<NearbyPlacesResponse> {
        val response = locationFacade.findNearbyPlaces(query, latitude, longitude, radius_km, limit)
        return ResponseEntity.ok(response)
    }
}
