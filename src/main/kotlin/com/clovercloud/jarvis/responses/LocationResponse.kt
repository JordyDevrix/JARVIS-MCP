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
    val source: String = "REAL",
    val retrievedAt: String = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now()),
    val note: String? = "Location snapshot is estimated to be valid for ~10-15 minutes or until the device moves or network changes."
)
