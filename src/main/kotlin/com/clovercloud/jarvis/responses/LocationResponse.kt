package com.clovercloud.jarvis.responses

data class LocationResponse(
    val city: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val description: String
)
