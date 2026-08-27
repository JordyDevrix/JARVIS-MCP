package com.clovercloud.jarvis.responses.flightradar

import com.clovercloud.jarvis.util.TimeUtil

data class NearbyFlight(
    val flightNumber: String?,
    val callsign: String?,
    val airline: String?,
    val aircraftType: String?,
    val registration: String?,
    val origin: String?,
    val destination: String?,
    val distanceKm: Double,
    val bearingFromObserver: String,
    val altitudeFeet: Int?,
    val groundSpeedKnots: Int?,
    val verticalSpeedFpm: Int?,
    val headingDegrees: Int?,
    val flightPhase: String,
    val latitude: Double,
    val longitude: Double
)

data class NearbyFlightsResponse(
    val observerLocation: String,
    val searchRadiusKm: Double,
    val flightsFoundCount: Int,
    val flights: List<NearbyFlight>,
    val retrievedAt: String = TimeUtil.amsterdamNowFormatted(),
    val note: String? = "Live airborne flight positions are estimated to be valid for ~15-30 seconds as aircraft are actively moving."
)
