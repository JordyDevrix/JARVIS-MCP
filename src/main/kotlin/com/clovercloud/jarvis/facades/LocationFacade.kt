package com.clovercloud.jarvis.facades

import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.responses.location.GeocodeResponse
import com.clovercloud.jarvis.responses.location.NearbyPlacesResponse
import com.clovercloud.jarvis.responses.location.ReverseGeocodeResponse
import com.clovercloud.jarvis.services.LocationService
import org.springframework.stereotype.Component

/**
 * Facade providing a unified entry point to the Location domain.
 * Decouples controllers and MCP tool handlers from underlying services and external clients.
 */
@Component
class LocationFacade(
    private val locationService: LocationService
) {

    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        zoom: Int? = 18
    ): ReverseGeocodeResponse {
        return locationService.reverseGeocode(latitude, longitude, zoom)
    }

    fun geocode(
        query: String,
        countryCode: String? = null,
        limit: Int? = 5
    ): GeocodeResponse {
        return locationService.geocode(query, countryCode, limit)
    }

    fun findNearbyPlaces(
        categoryOrQuery: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Double? = 1.0,
        limit: Int? = 10
    ): NearbyPlacesResponse {
        val lat = latitude ?: getCurrentLocation().latitude
        val lon = longitude ?: getCurrentLocation().longitude

        return locationService.findNearbyPlaces(
            categoryOrQuery = categoryOrQuery,
            latitude = lat,
            longitude = lon,
            radiusKm = radiusKm,
            limit = limit
        )
    }

    fun getCurrentLocation(ip: String? = null): LocationResponse {
        return locationService.getCurrentLocation(ip)
    }
}
