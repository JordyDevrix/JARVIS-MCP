package com.clovercloud.jarvis.tools

import com.clovercloud.jarvis.facades.LocationFacade
import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.responses.location.GeocodeResponse
import com.clovercloud.jarvis.responses.location.NearbyPlacesResponse
import com.clovercloud.jarvis.responses.location.ReverseGeocodeResponse
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class LocationTools(
    private val locationFacade: LocationFacade
) {

    @McpTool(
        name = "reverse_geocode",
        description = "Converts latitude and longitude GPS coordinates into a detailed human-readable address, city, postcode, country, and place details using OpenStreetMap."
    )
    fun reverseGeocode(
        @McpToolParam(description = "Latitude of the location (e.g. 52.3676)", required = true)
        latitude: Double,
        @McpToolParam(description = "Longitude of the location (e.g. 4.9041)", required = true)
        longitude: Double,
        @McpToolParam(description = "Level of detail zoom level: 3 (country), 10 (city), 16 (street), 18 (building). Default is 18.", required = false)
        zoom: Int?
    ): ReverseGeocodeResponse {
        return locationFacade.reverseGeocode(latitude, longitude, zoom)
    }

    @McpTool(
        name = "geocode_address",
        description = "Searches for an address, place, city, or landmark worldwide using OpenStreetMap. Returns geographic coordinates (latitude and longitude), address breakdown, and bounding box."
    )
    fun geocodeAddress(
        @McpToolParam(description = "Address, city, place name, or landmark to search (e.g. 'Dam Square Amsterdam', 'Eiffel Tower', 'Times Square New York')", required = true)
        query: String,
        @McpToolParam(description = "Optional two-letter country code filter (e.g. 'nl', 'us', 'de', 'fr')", required = false)
        country_code: String?,
        @McpToolParam(description = "Maximum number of search results to return (default 5, max 20)", required = false)
        limit: Int?
    ): GeocodeResponse {
        return locationFacade.geocode(query, country_code, limit)
    }

    @McpTool(
        name = "find_nearby_places",
        description = "Finds points of interest and amenities (such as hospitals, pharmacies, supermarkets, restaurants, EV chargers, parking, ATMs, police, or hotels) around coordinates. Returns distance, bearing, coordinates, and address."
    )
    fun findNearbyPlaces(
        @McpToolParam(description = "Category or amenity to find: 'pharmacy', 'hospital', 'supermarket', 'restaurant', 'cafe', 'charging_station', 'parking', 'atm', 'police', 'fuel', 'hotel', etc.", required = true)
        category: String,
        @McpToolParam(description = "Observer latitude. Defaults to current device/mock location if omitted.", required = false)
        latitude: Double?,
        @McpToolParam(description = "Observer longitude. Defaults to current device/mock location if omitted.", required = false)
        longitude: Double?,
        @McpToolParam(description = "Search radius in kilometers (default 1.0 km, max 25.0 km)", required = false)
        radius_km: Double?,
        @McpToolParam(description = "Maximum number of places to return (default 10, max 30)", required = false)
        limit: Int?
    ): NearbyPlacesResponse {
        return locationFacade.findNearbyPlaces(
            categoryOrQuery = category,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radius_km,
            limit = limit
        )
    }

    @McpTool(
        name = "get_current_location",
        description = "Determines the current geographic location (city, country, latitude, longitude, timezone) using real IP geolocation with configurable fallback."
    )
    fun getCurrentLocation(): LocationResponse {
        return locationFacade.getCurrentLocation()
    }

    fun lookupLocation(city: String?): LocationResponse {
        val q = city?.takeIf { it.isNotBlank() } ?: "Amsterdam"
        val geo = locationFacade.geocode(q, limit = 1)
        val first = geo.places.firstOrNull()
        return if (first != null) {
            LocationResponse(
                city = first.address?.city ?: first.address?.town ?: q,
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
    }
}
