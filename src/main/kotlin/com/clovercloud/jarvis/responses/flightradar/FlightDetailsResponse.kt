package com.clovercloud.jarvis.responses.flightradar

data class FlightDetailsResponse(
    val flightNumber: String?,
    val callsign: String?,
    val airline: String?,
    val aircraftType: String?,
    val registration: String?,
    val origin: String?,
    val destination: String?,
    val currentAltitudeFeet: Int?,
    val groundSpeedKnots: Int?,
    val headingDegrees: Int?,
    val flightPhase: String,
    val latitude: Double,
    val longitude: Double,
    val squawk: String?,
    val estimatedArrival: String?,
    val lastUpdatedUtc: String?,
    val note: String? = null
)
