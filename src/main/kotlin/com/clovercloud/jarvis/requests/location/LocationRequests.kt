package com.clovercloud.jarvis.requests.location

data class ReverseGeocodeRequest(
    val latitude: Double,
    val longitude: Double,
    val zoom: Int? = 18
)

data class GeocodeRequest(
    val query: String,
    val countryCode: String? = null,
    val limit: Int? = 5
)

data class NearbyPlacesRequest(
    val categoryOrQuery: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = 1.0,
    val limit: Int? = 10
)

data class CurrentLocationRequest(
    val ip: String? = null
)
