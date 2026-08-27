package com.clovercloud.jarvis.responses.location

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER_24H = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class OsmAddress(
    val road: String? = null,
    val houseNumber: String? = null,
    val suburb: String? = null,
    val neighbourhood: String? = null,
    val city: String? = null,
    val town: String? = null,
    val municipality: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countryCode: String? = null
)

data class ReverseGeocodeResponse(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val type: String? = null,
    val address: OsmAddress? = null,
    val osmType: String? = null,
    val osmId: Long? = null,
    val placeId: Long? = null,
    val boundingBox: List<Double>? = null,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Reverse geocoded address is static and estimated to be valid indefinitely."
)

data class GeocodePlaceItem(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val type: String? = null,
    val category: String? = null,
    val importance: Double? = null,
    val address: OsmAddress? = null,
    val boundingBox: List<Double>? = null
)

data class GeocodeResponse(
    val query: String,
    val totalResults: Int,
    val places: List<GeocodePlaceItem>,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Geocoded coordinates and address data are static and estimated to be valid indefinitely."
)

data class NearbyPlaceItem(
    val name: String,
    val category: String,
    val type: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val distanceKm: Double,
    val bearingFromObserver: String,
    val road: String? = null,
    val houseNumber: String? = null,
    val postcode: String? = null,
    val city: String? = null
)

data class NearbyPlacesResponse(
    val observerLocation: String,
    val searchCategory: String,
    val searchRadiusKm: Double,
    val totalFound: Int,
    val places: List<NearbyPlaceItem>,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Nearby amenities and points of interest are estimated to be valid for ~1-2 hours."
)
