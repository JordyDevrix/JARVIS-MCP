package com.clovercloud.jarvis.responses.flightradar

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER_24H = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class AirportFlightItem(
    val flightNumber: String?,
    val callsign: String?,
    val airline: String?,
    val aircraftType: String?,
    val origin: String?,
    val destination: String?,
    val altitudeFeet: Int?,
    val groundSpeedKnots: Int?,
    val latitude: Double,
    val longitude: Double
)

data class AirportFlightsResponse(
    val airportCode: String,
    val direction: String,
    val totalFlights: Int,
    val flights: List<AirportFlightItem>,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Airport arrival and departure flight schedules are estimated to be valid for ~1-2 minutes."
)
