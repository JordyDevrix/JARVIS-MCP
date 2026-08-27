package com.clovercloud.jarvis.responses.flightradar

data class RunwayInfo(
    val designator: String,
    val heading: Int?,
    val lengthMeters: Double?,
    val surface: String?
)

data class AirportDetailsResponse(
    val name: String,
    val iata: String?,
    val icao: String?,
    val city: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val elevationFeet: Double?,
    val timezone: String?,
    val timezoneOffset: String?,
    val runways: List<RunwayInfo>,
    val note: String? = null
)
