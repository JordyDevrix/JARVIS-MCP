package com.clovercloud.jarvis.responses.ov

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER_24H = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class LiveOvVehicle(
    val label: String?,
    val `operator`: String?,
    val operatorName: String?,
    val lineNumber: String?,
    val tripId: String?,
    val routeId: String?,
    val transportType: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val bearingFromObserver: String,
    val headingDegrees: Double?,
    val speedKmh: Double?,
    val currentStatus: String,
    val currentStopSequence: Int?,
    val timestampUtc: Long?
)

data class LiveOvLocationsResponse(
    val observerLocation: String,
    val searchRadiusKm: Double,
    val vehiclesFoundCount: Int,
    val vehicles: List<LiveOvVehicle>,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Live vehicle GPS coordinates are real-time telemetry and estimated to be valid for ~15-30 seconds as vehicles are in transit."
)

data class OvDepartureItem(
    val linePublicNumber: String?,
    val lineName: String?,
    val destination: String?,
    val transportType: String,
    val operatorCode: String?,
    val targetDepartureTime: String?,
    val expectedDepartureTime: String?,
    val delayMinutes: Long?,
    val platform: String?,
    val tripStopStatus: String?,
    val wheelChairAccessible: String?,
    val journeyNumber: String?
)

data class OvStopDeparturesResponse(
    val stopAreaCode: String,
    val stopName: String,
    val town: String?,
    val latitude: Double?,
    val longitude: Double?,
    val totalDepartures: Int,
    val departures: List<OvDepartureItem>,
    val disruptions: List<String> = emptyList(),
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Live departure times, platform assignments, and delay predictions are estimated to be valid for ~30-60 seconds."
)

data class OvStopItem(
    val stopAreaCode: String,
    val name: String,
    val town: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double? = null
)

data class OvStopsSearchResponse(
    val query: String?,
    val totalMatches: Int,
    val stops: List<OvStopItem>,
    val retrievedAt: String = FORMATTER_24H.format(LocalDateTime.now()),
    val note: String? = "Public transport stop and station data is estimated to be valid for ~24 hours."
)
