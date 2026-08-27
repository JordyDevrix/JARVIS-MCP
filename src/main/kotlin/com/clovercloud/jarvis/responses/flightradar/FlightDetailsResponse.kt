package com.clovercloud.jarvis.responses.flightradar

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER_24H = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Live flight tracking telemetry and status are estimated to be valid for ~30-60 seconds."
)
