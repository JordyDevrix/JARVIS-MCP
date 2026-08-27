package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.FlightradarClient
import com.clovercloud.jarvis.clients.dto.Fr24FlightPosition
import com.clovercloud.jarvis.responses.flightradar.AirportDetailsResponse
import com.clovercloud.jarvis.responses.flightradar.AirportFlightItem
import com.clovercloud.jarvis.responses.flightradar.AirportFlightsResponse
import com.clovercloud.jarvis.responses.flightradar.FlightDetailsResponse
import com.clovercloud.jarvis.responses.flightradar.NearbyFlight
import com.clovercloud.jarvis.responses.flightradar.NearbyFlightsResponse
import com.clovercloud.jarvis.responses.flightradar.RunwayInfo
import org.springframework.stereotype.Service
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class FlightradarService(
    private val client: FlightradarClient
) {

    fun getNearbyFlights(
        radiusKm: Double,
        observerLat: Double,
        observerLon: Double,
        limit: Int = 20
    ): NearbyFlightsResponse {
        val observerDesc = "Lat: %.4f, Lon: %.4f".format(Locale.US, observerLat, observerLon)
        val cleanRadius = radiusKm.coerceIn(0.5, 200.0)
        val cleanLimit = limit.coerceIn(1, 100)
        val validityNote = "Live airborne flight positions are estimated to be valid for ~15-30 seconds as aircraft are actively moving."

        if (!client.isConfigured()) {
            return NearbyFlightsResponse(
                observerLocation = observerDesc,
                searchRadiusKm = cleanRadius,
                flightsFoundCount = 0,
                flights = emptyList(),
                note = "Flightradar24 API token is not configured. Please set the FR24_API_TOKEN environment variable. $validityNote"
            )
        }

        // Calculate bounding box: order north, south, west, east
        val deltaLat = cleanRadius / 111.0
        val cosLat = cos(Math.toRadians(observerLat)).coerceAtLeast(0.01)
        val deltaLon = cleanRadius / (111.0 * cosLat)

        val north = (observerLat + deltaLat).coerceAtMost(90.0)
        val south = (observerLat - deltaLat).coerceAtLeast(-90.0)
        val west = ((observerLon - deltaLon + 540.0) % 360.0) - 180.0
        val east = ((observerLon + deltaLon + 540.0) % 360.0) - 180.0

        val boundsParam = "%.3f,%.3f,%.3f,%.3f".format(Locale.US, north, south, west, east)

        val rawResponse = client.getLiveFlightPositions(
            mapOf(
                "bounds" to boundsParam,
                "limit" to (cleanLimit * 2).toString()
            )
        )

        val rawFlights = rawResponse?.data ?: emptyList()
        val mappedFlights = rawFlights.map { plane ->
            val dist = haversineDistanceKm(observerLat, observerLon, plane.lat, plane.lon)
            val bearing = calculateBearing(observerLat, observerLon, plane.lat, plane.lon)
            val phase = determineFlightPhase(plane.vspeed, plane.alt)
            plane to (round(dist * 100.0) / 100.0) to bearing to phase
        }
            .filter { it.first.first.second <= cleanRadius }
            .sortedBy { it.first.first.second }
            .take(cleanLimit)
            .map { (info, phase) ->
                val (planeAndDist, bearing) = info
                val (plane, dist) = planeAndDist
                NearbyFlight(
                    flightNumber = plane.flight?.takeIf { it.isNotBlank() },
                    callsign = plane.callsign?.takeIf { it.isNotBlank() },
                    airline = plane.operatingAs ?: plane.paintedAs,
                    aircraftType = plane.type,
                    registration = plane.reg,
                    origin = plane.origIata ?: plane.origIcao,
                    destination = plane.destIata ?: plane.destIcao,
                    distanceKm = dist,
                    bearingFromObserver = bearing,
                    altitudeFeet = plane.alt,
                    groundSpeedKnots = plane.gspeed,
                    verticalSpeedFpm = plane.vspeed,
                    headingDegrees = plane.track,
                    flightPhase = phase,
                    latitude = plane.lat,
                    longitude = plane.lon
                )
            }

        val note = when {
            mappedFlights.isNotEmpty() -> validityNote
            rawFlights.isNotEmpty() -> {
                val closest = rawFlights.minByOrNull { haversineDistanceKm(observerLat, observerLon, it.lat, it.lon) }
                val closestDist = if (closest != null) haversineDistanceKm(observerLat, observerLon, closest.lat, closest.lon) else 0.0
                val isSandboxFlight = closest?.flight == "SK7679" || closest?.fr24Id == "333ca4a2"
                if (isSandboxFlight) {
                    "No aircraft currently airborne within $cleanRadius km. (Detected sandbox test flight SK7679 at %.1f km away. Note: Your current API token is a Flightradar24 Sandbox key which returns static test flight SK7679. Replace it with a Production API key in .env to track live flights). $validityNote".format(Locale.US, closestDist)
                } else {
                    "No aircraft currently airborne within $cleanRadius km of this location. (Closest detected aircraft is ${closest?.flight ?: closest?.callsign} at %.1f km away). $validityNote".format(Locale.US, closestDist)
                }
            }
            else -> "No aircraft currently airborne within $cleanRadius km of this location. $validityNote"
        }

        return NearbyFlightsResponse(
            observerLocation = observerDesc,
            searchRadiusKm = cleanRadius,
            flightsFoundCount = mappedFlights.size,
            flights = mappedFlights,
            note = note
        )
    }

    fun getFlightDetails(query: String): FlightDetailsResponse {
        val cleanQuery = query.trim().uppercase()

        val validityNote = "Live flight tracking telemetry and status are estimated to be valid for ~30-60 seconds."

        if (!client.isConfigured()) {
            return FlightDetailsResponse(
                flightNumber = cleanQuery,
                callsign = null,
                airline = null,
                aircraftType = null,
                registration = null,
                origin = null,
                destination = null,
                currentAltitudeFeet = null,
                groundSpeedKnots = null,
                headingDegrees = null,
                flightPhase = "Unknown",
                latitude = 0.0,
                longitude = 0.0,
                squawk = null,
                estimatedArrival = null,
                lastUpdatedUtc = null,
                note = "Flightradar24 API token is not configured. Please set the FR24_API_TOKEN environment variable. $validityNote"
            )
        }

        // Try searching by flight number first
        var response = client.getLiveFlightPositions(mapOf("flights" to cleanQuery, "limit" to "5"))
        var flight = response?.data?.firstOrNull()

        // If not found, try by callsign
        if (flight == null) {
            response = client.getLiveFlightPositions(mapOf("callsigns" to cleanQuery, "limit" to "5"))
            flight = response?.data?.firstOrNull()
        }

        // If not found, try by registration
        if (flight == null) {
            response = client.getLiveFlightPositions(mapOf("registrations" to cleanQuery, "limit" to "5"))
            flight = response?.data?.firstOrNull()
        }

        if (flight == null) {
            return FlightDetailsResponse(
                flightNumber = cleanQuery,
                callsign = null,
                airline = null,
                aircraftType = null,
                registration = null,
                origin = null,
                destination = null,
                currentAltitudeFeet = null,
                groundSpeedKnots = null,
                headingDegrees = null,
                flightPhase = "Not Found",
                latitude = 0.0,
                longitude = 0.0,
                squawk = null,
                estimatedArrival = null,
                lastUpdatedUtc = null,
                note = "Flight '$cleanQuery' is not currently active or not found in real-time radar data. $validityNote"
            )
        }

        val phase = determineFlightPhase(flight.vspeed, flight.alt)

        return FlightDetailsResponse(
            flightNumber = flight.flight ?: cleanQuery,
            callsign = flight.callsign,
            airline = flight.operatingAs ?: flight.paintedAs,
            aircraftType = flight.type,
            registration = flight.reg,
            origin = flight.origIata ?: flight.origIcao,
            destination = flight.destIata ?: flight.destIcao,
            currentAltitudeFeet = flight.alt,
            groundSpeedKnots = flight.gspeed,
            headingDegrees = flight.track,
            flightPhase = phase,
            latitude = flight.lat,
            longitude = flight.lon,
            squawk = flight.squawk,
            estimatedArrival = flight.eta,
            lastUpdatedUtc = flight.timestamp,
            note = validityNote
        )
    }

    fun getAirportFlights(airportCode: String, direction: String? = "both", limit: Int = 20): AirportFlightsResponse {
        val cleanCode = airportCode.trim().uppercase()
        val cleanDir = when (direction?.trim()?.lowercase()) {
            "inbound" -> "inbound"
            "outbound" -> "outbound"
            else -> "both"
        }
        val cleanLimit = limit.coerceIn(1, 100)
        val validityNote = "Airport arrival and departure flight schedules are estimated to be valid for ~1-2 minutes."

        if (!client.isConfigured()) {
            return AirportFlightsResponse(
                airportCode = cleanCode,
                direction = cleanDir,
                totalFlights = 0,
                flights = emptyList(),
                note = "Flightradar24 API token is not configured. Please set the FR24_API_TOKEN environment variable. $validityNote"
            )
        }

        val response = client.getLiveFlightPositions(
            mapOf(
                "airports" to "$cleanDir:$cleanCode",
                "limit" to cleanLimit.toString()
            )
        )

        val flights = response?.data?.map { plane ->
            AirportFlightItem(
                flightNumber = plane.flight?.takeIf { it.isNotBlank() },
                callsign = plane.callsign?.takeIf { it.isNotBlank() },
                airline = plane.operatingAs ?: plane.paintedAs,
                aircraftType = plane.type,
                origin = plane.origIata ?: plane.origIcao,
                destination = plane.destIata ?: plane.destIcao,
                altitudeFeet = plane.alt,
                groundSpeedKnots = plane.gspeed,
                latitude = plane.lat,
                longitude = plane.lon
            )
        } ?: emptyList()

        return AirportFlightsResponse(
            airportCode = cleanCode,
            direction = cleanDir,
            totalFlights = flights.size,
            flights = flights,
            note = if (flights.isEmpty()) "No active $cleanDir flights found for airport $cleanCode. $validityNote" else validityNote
        )
    }

    fun getAirportDetails(airportCode: String): AirportDetailsResponse {
        val cleanCode = airportCode.trim().uppercase()

        val validityNote = "Airport specifications and runway data are static and estimated to be valid for several months."

        if (!client.isConfigured()) {
            return AirportDetailsResponse(
                name = cleanCode,
                iata = null,
                icao = null,
                city = null,
                country = null,
                countryCode = null,
                latitude = 0.0,
                longitude = 0.0,
                elevationFeet = null,
                timezone = null,
                timezoneOffset = null,
                runways = emptyList(),
                note = "Flightradar24 API token is not configured. Please set the FR24_API_TOKEN environment variable. $validityNote"
            )
        }

        val airport = client.getAirportFull(cleanCode)

        if (airport == null) {
            return AirportDetailsResponse(
                name = cleanCode,
                iata = null,
                icao = null,
                city = null,
                country = null,
                countryCode = null,
                latitude = 0.0,
                longitude = 0.0,
                elevationFeet = null,
                timezone = null,
                timezoneOffset = null,
                runways = emptyList(),
                note = "Airport '$cleanCode' not found. $validityNote"
            )
        }

        val runwayInfos = airport.runways.map { r ->
            val surf = when (val s = r.surface) {
                is Map<*, *> -> (s["description"] ?: s["type"])?.toString()
                else -> s?.toString()
            }
            RunwayInfo(
                designator = r.name ?: r.designator ?: "Unknown",
                heading = r.heading,
                lengthMeters = r.length,
                surface = surf
            )
        }

        return AirportDetailsResponse(
            name = airport.name,
            iata = airport.iata,
            icao = airport.icao,
            city = airport.city,
            country = airport.country?.name,
            countryCode = airport.country?.code,
            latitude = airport.lat,
            longitude = airport.lon,
            elevationFeet = airport.elevation,
            timezone = airport.timezone?.name,
            timezoneOffset = airport.timezone?.offset?.toString(),
            runways = runwayInfos,
            note = validityNote
        )
    }

    private fun determineFlightPhase(vspeed: Int?, alt: Int?): String {
        val v = vspeed ?: 0
        val a = alt ?: 0
        return when {
            a < 1000 && v == 0 -> "On Ground / Taxiing"
            v > 250 -> "Climbing (+${v} ft/min)"
            v < -250 -> "Descending (${v} ft/min)"
            else -> "Cruising / Level Flight"
        }
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
