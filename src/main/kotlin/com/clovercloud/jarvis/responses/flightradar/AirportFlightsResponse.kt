package com.clovercloud.jarvis.responses.flightradar

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
    val note: String? = null
)
