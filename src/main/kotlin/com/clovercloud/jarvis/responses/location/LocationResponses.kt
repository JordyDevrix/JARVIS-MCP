package com.clovercloud.jarvis.responses.location

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
    val note: String? = null
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
    val note: String? = null
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
    val note: String? = null
)
