package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.OvClient
import com.clovercloud.jarvis.config.OvProperties
import com.clovercloud.jarvis.responses.ov.LiveOvLocationsResponse
import com.clovercloud.jarvis.responses.ov.LiveOvVehicle
import com.clovercloud.jarvis.responses.ov.OvDepartureItem
import com.clovercloud.jarvis.responses.ov.OvStopDeparturesResponse
import com.clovercloud.jarvis.responses.ov.OvStopItem
import com.clovercloud.jarvis.responses.ov.OvStopsSearchResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class OvService(
    private val client: OvClient,
    private val properties: OvProperties
) {
    private val logger = LoggerFactory.getLogger(OvService::class.java)
    private val objectMapper = ObjectMapper()

    // Vehicle positions cache (short TTL)
    @Volatile private var cachedVehicles: List<LiveOvVehicle>? = null
    @Volatile private var vehicleCacheTime: Long = 0L

    // Stops directory cache (long TTL)
    @Volatile private var cachedStops: List<OvStopItem>? = null
    @Volatile private var stopsCacheTime: Long = 0L
    private val stopCodeMap = ConcurrentHashMap<String, OvStopItem>()

    fun getLiveVehicleLocations(
        radiusKm: Double,
        observerLat: Double,
        observerLon: Double,
        operator: String? = null,
        transportType: String? = null,
        lineNumber: String? = null,
        limit: Int = 25
    ): LiveOvLocationsResponse {
        val cleanRadius = radiusKm.coerceIn(0.5, 100.0)
        val cleanLimit = limit.coerceIn(1, 100)
        val observerDesc = "Lat: %.4f, Lon: %.4f".format(Locale.US, observerLat, observerLon)

        val allVehicles = getOrRefreshVehicles()

        if (allVehicles.isEmpty()) {
            return LiveOvLocationsResponse(
                observerLocation = observerDesc,
                searchRadiusKm = cleanRadius,
                vehiclesFoundCount = 0,
                vehicles = emptyList(),
                note = "Could not retrieve live vehicle positions from OVapi GTFS-RT feed at this moment."
            )
        }

        val operatorFilter = operator?.trim()?.uppercase()
        val typeFilter = transportType?.trim()?.uppercase()
        val lineFilter = lineNumber?.trim()?.lowercase()

        val matchingVehicles = allVehicles.map { vehicle ->
            val dist = haversineDistanceKm(observerLat, observerLon, vehicle.latitude, vehicle.longitude)
            val bearing = calculateBearing(observerLat, observerLon, vehicle.latitude, vehicle.longitude)
            vehicle.copy(
                distanceKm = round(dist * 100.0) / 100.0,
                bearingFromObserver = bearing
            )
        }
            .filter { it.distanceKm <= cleanRadius }
            .filter { v -> operatorFilter == null || v.operator?.uppercase() == operatorFilter || v.operatorName?.uppercase()?.contains(operatorFilter) == true }
            .filter { v -> typeFilter == null || typeFilter == "ALL" || v.transportType.uppercase() == typeFilter }
            .filter { v -> lineFilter == null || v.lineNumber?.lowercase()?.contains(lineFilter) == true }
            .sortedBy { it.distanceKm }
            .take(cleanLimit)

        val note = when {
            matchingVehicles.isNotEmpty() -> null
            else -> {
                val closest = allVehicles.minByOrNull { haversineDistanceKm(observerLat, observerLon, it.latitude, it.longitude) }
                val closestDist = if (closest != null) haversineDistanceKm(observerLat, observerLon, closest.latitude, closest.longitude) else 0.0
                "No public transport vehicles found within $cleanRadius km matching criteria. (Closest active vehicle is ${closest?.operator ?: ""} line ${closest?.lineNumber ?: ""} at %.1f km away). Note: Dutch trains (NS) do not broadcast raw GPS coords; live train departures and platform info are available via stop departures.".format(Locale.US, closestDist)
            }
        }

        return LiveOvLocationsResponse(
            observerLocation = observerDesc,
            searchRadiusKm = cleanRadius,
            vehiclesFoundCount = matchingVehicles.size,
            vehicles = matchingVehicles,
            note = note
        )
    }

    fun getStopDepartures(
        stopAreaCodeOrQuery: String,
        transportType: String? = null,
        limit: Int = 20
    ): OvStopDeparturesResponse {
        val cleanInput = stopAreaCodeOrQuery.trim()
        val cleanLimit = limit.coerceIn(1, 50)
        val typeFilter = transportType?.trim()?.uppercase()

        // Resolve stop code if input is a station/stop name
        val resolvedStop = resolveStopArea(cleanInput)
        val stopCode = resolvedStop?.stopAreaCode ?: cleanInput

        val rawJson = client.getStopAreaDepartures(stopCode)
        if (rawJson.isNullOrBlank()) {
            return OvStopDeparturesResponse(
                stopAreaCode = stopCode,
                stopName = resolvedStop?.name ?: cleanInput,
                town = resolvedStop?.town,
                latitude = resolvedStop?.latitude,
                longitude = resolvedStop?.longitude,
                totalDepartures = 0,
                departures = emptyList(),
                note = "Stop area '$stopCode' not found or currently has no active departure information."
            )
        }

        return try {
            val rootNode = objectMapper.readTree(rawJson)
            val stopDataNode = rootNode.path(stopCode)

            val departures = mutableListOf<OvDepartureItem>()
            val disruptions = mutableListOf<String>()
            var stopName = resolvedStop?.name
            var stopTown = resolvedStop?.town
            var stopLat = resolvedStop?.latitude
            var stopLon = resolvedStop?.longitude

            if (stopDataNode.isObject) {
                stopDataNode.properties().forEach { (_, tpcNode) ->
                    // Extract Stop metadata if available
                    val stopNode = tpcNode.path("Stop")
                    if (stopNode.isObject) {
                        if (stopName == null) stopName = stopNode.path("TimingPointName").asText(null)
                        if (stopTown == null) stopTown = stopNode.path("TimingPointTown").asText(null)
                        if (stopLat == null) stopLat = stopNode.path("Latitude").asDouble(0.0).takeIf { it != 0.0 }
                        if (stopLon == null) stopLon = stopNode.path("Longitude").asDouble(0.0).takeIf { it != 0.0 }
                    }

                    // Extract General Messages / Disruptions
                    val messagesNode = tpcNode.path("GeneralMessages")
                    if (messagesNode.isObject) {
                        messagesNode.properties().forEach { (_, msgNode) ->
                            val content = msgNode.path("MessageContent").asText(null)
                            if (!content.isNullOrBlank() && !disruptions.contains(content)) {
                                disruptions.add(content)
                            }
                        }
                    }

                    // Extract Passes
                    val passesNode = tpcNode.path("Passes")
                    if (passesNode.isObject) {
                        passesNode.properties().forEach { (_, passNode) ->
                            val tType = passNode.path("TransportType").asText("BUS").uppercase()
                            if (typeFilter == null || typeFilter == "ALL" || tType == typeFilter) {
                                val targetDep = passNode.path("TargetDepartureTime").asText(null)
                                    ?: passNode.path("TargetArrivalTime").asText(null)
                                val expectedDep = passNode.path("ExpectedDepartureTime").asText(null)
                                    ?: passNode.path("ExpectedArrivalTime").asText(null)

                                val delayMinutes = calculateDelayMinutes(targetDep, expectedDep)

                                departures.add(
                                    OvDepartureItem(
                                        linePublicNumber = passNode.path("LinePublicNumber").asText(null)
                                            ?: passNode.path("LinePlanningNumber").asText(null),
                                        lineName = passNode.path("LineName").asText(null),
                                        destination = passNode.path("DestinationName50").asText(null),
                                        transportType = tType,
                                        operatorCode = passNode.path("DataOwnerCode").asText(null)
                                            ?: passNode.path("OperatorCode").asText(null),
                                        targetDepartureTime = targetDep,
                                        expectedDepartureTime = expectedDep,
                                        delayMinutes = delayMinutes,
                                        platform = passNode.path("SideCode").asText(null)?.takeIf { it != "-" },
                                        tripStopStatus = passNode.path("TripStopStatus").asText(null),
                                        wheelChairAccessible = passNode.path("WheelChairAccessible").asText(null),
                                        journeyNumber = passNode.path("JourneyNumber").asText(null)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val sortedDepartures = departures
                .sortedWith(compareBy({ it.expectedDepartureTime ?: it.targetDepartureTime ?: "" }, { it.linePublicNumber ?: "" }))
                .take(cleanLimit)

            OvStopDeparturesResponse(
                stopAreaCode = stopCode,
                stopName = stopName ?: cleanInput,
                town = stopTown,
                latitude = stopLat,
                longitude = stopLon,
                totalDepartures = sortedDepartures.size,
                departures = sortedDepartures,
                disruptions = disruptions,
                note = if (sortedDepartures.isEmpty()) "No departures currently scheduled or active for stop '$stopCode'." else null
            )
        } catch (e: Exception) {
            logger.error("Failed to parse stop departures for '$stopCode': ${e.message}", e)
            OvStopDeparturesResponse(
                stopAreaCode = stopCode,
                stopName = resolvedStop?.name ?: cleanInput,
                town = resolvedStop?.town,
                latitude = resolvedStop?.latitude,
                longitude = resolvedStop?.longitude,
                totalDepartures = 0,
                departures = emptyList(),
                note = "Error parsing departure data for stop '$stopCode'."
            )
        }
    }

    fun searchStops(
        query: String? = null,
        observerLat: Double? = null,
        observerLon: Double? = null,
        radiusKm: Double? = 5.0,
        limit: Int = 15
    ): OvStopsSearchResponse {
        val cleanLimit = limit.coerceIn(1, 50)
        val cleanQuery = query?.trim()
        val allStops = getOrRefreshStops()

        if (allStops.isEmpty()) {
            return OvStopsSearchResponse(
                query = cleanQuery,
                totalMatches = 0,
                stops = emptyList(),
                note = "Stops index is currently unavailable from OVapi."
            )
        }

        var results = allStops

        // Filter by text query if provided
        if (!cleanQuery.isNullOrBlank()) {
            val q = cleanQuery.lowercase()
            results = results.filter { stop ->
                stop.name.lowercase().contains(q) ||
                stop.town.lowercase().contains(q) ||
                stop.stopAreaCode.lowercase() == q
            }
        }

        // Proximity calculation if coordinates provided
        if (observerLat != null && observerLon != null) {
            val cleanRadius = (radiusKm ?: 5.0).coerceIn(0.2, 50.0)
            results = results.map { stop ->
                val dist = haversineDistanceKm(observerLat, observerLon, stop.latitude, stop.longitude)
                stop.copy(distanceKm = round(dist * 100.0) / 100.0)
            }
                .filter { (it.distanceKm ?: Double.MAX_VALUE) <= cleanRadius }
                .sortedBy { it.distanceKm }
        } else if (!cleanQuery.isNullOrBlank()) {
            // Prioritize exact matches in stop area code or beginning of name
            results = results.sortedWith(
                compareBy(
                    { !it.stopAreaCode.equals(cleanQuery, ignoreCase = true) },
                    { !it.name.startsWith(cleanQuery, ignoreCase = true) },
                    { it.name }
                )
            )
        }

        val finalStops = results.take(cleanLimit)

        return OvStopsSearchResponse(
            query = cleanQuery,
            totalMatches = finalStops.size,
            stops = finalStops,
            note = if (finalStops.isEmpty()) "No public transport stops found matching '$cleanQuery'." else null
        )
    }

    @Synchronized
    private fun getOrRefreshVehicles(): List<LiveOvVehicle> {
        val now = System.currentTimeMillis()
        val cacheTtlMs = properties.vehicleCacheTtlSeconds * 1000L

        if (cachedVehicles != null && (now - vehicleCacheTime) < cacheTtlMs) {
            return cachedVehicles!!
        }

        val bytes = client.getVehiclePositionsPb()
        if (bytes == null || bytes.isEmpty()) {
            return cachedVehicles ?: emptyList()
        }

        try {
            val feed = GtfsRealtime.FeedMessage.parseFrom(bytes)
            val vehicles = mutableListOf<LiveOvVehicle>()

            for (entity in feed.entityList) {
                if (!entity.hasVehicle()) continue
                val v = entity.vehicle
                val pos = v.position ?: continue

                val lat = pos.latitude.toDouble()
                val lon = pos.longitude.toDouble()

                // Validate coordinates roughly within Netherlands bounding box
                if (lat < 50.0 || lat > 54.5 || lon < 3.0 || lon > 7.5) continue

                // Entity ID format: "YYYY-MM-DD:Operator:LinePlanningNumber:JourneyNumber"
                val idParts = entity.id.split(":")
                val operatorCode = if (idParts.size >= 2) idParts[1] else null
                val linePlanning = if (idParts.size >= 3) idParts[2] else null

                val label = v.vehicle?.label?.takeIf { it.isNotBlank() }
                    ?: v.vehicle?.id?.takeIf { it.isNotBlank() }

                val speedKmh = if (pos.hasSpeed() && pos.speed > 0f) {
                    round(pos.speed * 3.6 * 10.0) / 10.0
                } else null

                val heading = if (pos.hasBearing() && pos.bearing > 0f) {
                    round(pos.bearing.toDouble() * 10.0) / 10.0
                } else null

                val status = when (v.currentStatus) {
                    VehiclePosition.VehicleStopStatus.INCOMING_AT -> "INCOMING_AT"
                    VehiclePosition.VehicleStopStatus.STOPPED_AT -> "STOPPED_AT"
                    VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO -> "IN_TRANSIT_TO"
                    else -> "IN_TRANSIT"
                }

                val operatorName = getOperatorFriendlyName(operatorCode)
                val transportType = inferTransportType(operatorCode, linePlanning)

                vehicles.add(
                    LiveOvVehicle(
                        label = label,
                        operator = operatorCode,
                        operatorName = operatorName,
                        lineNumber = linePlanning,
                        tripId = v.trip?.tripId?.takeIf { it.isNotBlank() },
                        routeId = v.trip?.routeId?.takeIf { it.isNotBlank() },
                        transportType = transportType,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = 0.0,
                        bearingFromObserver = "",
                        headingDegrees = heading,
                        speedKmh = speedKmh,
                        currentStatus = status,
                        currentStopSequence = if (v.hasCurrentStopSequence()) v.currentStopSequence else null,
                        timestampUtc = if (v.hasTimestamp()) v.timestamp else null
                    )
                )
            }

            cachedVehicles = vehicles
            vehicleCacheTime = now
            logger.info("Refreshed Dutch OV vehicle positions: ${vehicles.size} active vehicles in service")
            return vehicles
        } catch (e: Exception) {
            logger.error("Failed to decode vehiclePositions.pb feed: ${e.message}", e)
            return cachedVehicles ?: emptyList()
        }
    }

    @Synchronized
    private fun getOrRefreshStops(): List<OvStopItem> {
        val now = System.currentTimeMillis()
        val cacheTtlMs = properties.stopCacheTtlHours * 3600_000L

        if (cachedStops != null && (now - stopsCacheTime) < cacheTtlMs) {
            return cachedStops!!
        }

        val json = client.getAllStopAreas()
        if (json.isNullOrBlank()) {
            return cachedStops ?: emptyList()
        }

        try {
            val root = objectMapper.readTree(json)
            val stops = mutableListOf<OvStopItem>()

            if (root.isObject) {
                root.properties().forEach { (code, node) ->
                    val name = node.path("TimingPointName").asText(code)
                    val town = node.path("TimingPointTown").asText("")
                    val lat = node.path("Latitude").asDouble(0.0)
                    val lon = node.path("Longitude").asDouble(0.0)

                    // Skip coordinate-less / corrupted mock stops if present
                    if (lat > 50.0 && lat < 55.0 && lon > 2.5 && lon < 8.0) {
                        val item = OvStopItem(
                            stopAreaCode = code,
                            name = name,
                            town = town,
                            latitude = lat,
                            longitude = lon
                        )
                        stops.add(item)
                        stopCodeMap[code.lowercase()] = item
                    }
                }
            }

            cachedStops = stops
            stopsCacheTime = now
            logger.info("Cached ${stops.size} Dutch OV stop areas for fast search")
            return stops
        } catch (e: Exception) {
            logger.error("Failed to parse stop areas: ${e.message}", e)
            return cachedStops ?: emptyList()
        }
    }

    private fun resolveStopArea(input: String): OvStopItem? {
        val clean = input.trim()
        // Ensure stops cache is loaded
        getOrRefreshStops()

        // 1. Direct code match
        stopCodeMap[clean.lowercase()]?.let { return it }

        // 2. Search by exact or partial name
        val stops = cachedStops ?: return null
        return stops.firstOrNull { it.stopAreaCode.equals(clean, ignoreCase = true) }
            ?: stops.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: stops.firstOrNull { it.name.contains(clean, ignoreCase = true) }
            ?: stops.firstOrNull { it.town.equals(clean, ignoreCase = true) }
    }

    private fun calculateDelayMinutes(target: String?, expected: String?): Long? {
        if (target == null || expected == null) return null
        return try {
            val t = parseDateTime(target)
            val e = parseDateTime(expected)
            if (t != null && e != null) Duration.between(t, e).toMinutes() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDateTime(raw: String): java.time.LocalDateTime? {
        return try {
            if (raw.contains("+") || raw.endsWith("Z") || (raw.lastIndexOf("-") > 10)) {
                OffsetDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
            } else {
                java.time.LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getOperatorFriendlyName(code: String?): String? = when (code?.uppercase()) {
        "ARR" -> "Arriva"
        "CXX" -> "Connexxion"
        "DELIJN" -> "De Lijn"
        "EBS" -> "EBS"
        "GVB" -> "GVB Amsterdam"
        "HTM" -> "HTM Den Haag"
        "KEOLIS" -> "Keolis"
        "QBUZZ" -> "Qbuzz"
        "RET" -> "RET Rotterdam"
        "NS" -> "Nederlandse Spoorwegen"
        else -> code
    }

    private fun inferTransportType(operator: String?, line: String?): String = when {
        operator == "GVB" && (line?.matches(Regex("^5[0-4]$")) == true) -> "METRO"
        operator == "RET" && (line?.matches(Regex("^[A-E]$")) == true) -> "METRO"
        operator == "GVB" && (line?.matches(Regex("^[1-9]|1[0-9]|2[0-7]$")) == true) -> "TRAM"
        operator == "HTM" && (line?.matches(Regex("^[1-9]|1[0-9]$")) == true) -> "TRAM"
        operator == "RET" && (line?.matches(Regex("^2[0-9]$")) == true) -> "TRAM"
        line?.startsWith("F", ignoreCase = true) == true -> "FERRY"
        else -> "BUS"
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearingDegrees = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        val index = (round(bearingDegrees / 45.0).toInt()) % 8
        return directions[index]
    }
}
