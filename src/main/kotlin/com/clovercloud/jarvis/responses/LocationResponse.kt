package com.clovercloud.jarvis.responses

data class LocationResponse(
    val city: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val description: String,
    val region: String? = null,
    val postalCode: String? = null,
    val source: String = "REAL"
)
