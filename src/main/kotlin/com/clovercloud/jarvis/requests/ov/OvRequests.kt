package com.clovercloud.jarvis.requests.ov

data class LiveOvLocationsRequest(
    val radiusKm: Double? = 5.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val `operator`: String? = null,
    val transportType: String? = null,
    val lineNumber: String? = null,
    val limit: Int? = 25
)

data class OvStopDeparturesRequest(
    val stopAreaCode: String,
    val transportType: String? = null,
    val limit: Int? = 20
)

data class SearchOvStopsRequest(
    val query: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = 5.0,
    val limit: Int? = 15
)
