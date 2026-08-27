package com.clovercloud.jarvis.tools

import com.clovercloud.jarvis.responses.flightradar.AirportDetailsResponse
import com.clovercloud.jarvis.responses.flightradar.AirportFlightsResponse
import com.clovercloud.jarvis.responses.flightradar.FlightDetailsResponse
import com.clovercloud.jarvis.responses.flightradar.NearbyFlightsResponse
import com.clovercloud.jarvis.services.FlightradarService
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class FlightradarTools(
    private val flightradarService: FlightradarService,
    private val locationTools: LocationTools
) {

    @McpTool(
        name = "get_nearby_flights",
        description = "Finds real-time airborne aircraft within a given radius (km) of a location. Returns closest flights with distance from observer, altitude, speed, origin, and destination."
    )
    fun getNearbyFlights(
        @McpToolParam(
            description = "Search radius in kilometers (e.g. 2.0, 5.0, 10.0, 25.0). Defaults to 10.0 km.",
            required = false
        )
        radius_km: Double?,
        @McpToolParam(
            description = "Observer latitude. Defaults to current device/system latitude if omitted.",
            required = false
        )
        latitude: Double?,
        @McpToolParam(
            description = "Observer longitude. Defaults to current device/system longitude if omitted.",
            required = false
        )
        longitude: Double?,
        @McpToolParam(
            description = "Maximum number of flights to return (default 20, max 100).",
            required = false
        )
        limit: Int?
    ): NearbyFlightsResponse {
        val radius = radius_km ?: 10.0
        val currentLoc = if (latitude == null || longitude == null) locationTools.getCurrentLocation() else null
        val lat = latitude ?: currentLoc?.latitude ?: 52.3676
        val lon = longitude ?: currentLoc?.longitude ?: 4.9041
        val maxLimit = limit ?: 20

        return flightradarService.getNearbyFlights(
            radiusKm = radius,
            observerLat = lat,
            observerLon = lon,
            limit = maxLimit
        )
    }

    @McpTool(
        name = "get_flight_details",
        description = "Retrieves live flight tracking details by commercial flight number (e.g. 'KL1234'), callsign (e.g. 'KLM123'), or aircraft registration (e.g. 'PH-BVA')."
    )
    fun getFlightDetails(
        @McpToolParam(
            description = "Commercial flight number (e.g. KL1234), ATC callsign, or aircraft registration.",
            required = true
        )
        query: String
    ): FlightDetailsResponse {
        return flightradarService.getFlightDetails(query)
    }

    @McpTool(
        name = "get_airport_flights",
        description = "Lists real-time inbound or outbound flights for an airport (e.g. 'AMS', 'LHR', 'JFK')."
    )
    fun getAirportFlights(
        @McpToolParam(
            description = "IATA or ICAO airport code (e.g. AMS, EHAM, JFK, LHR).",
            required = true
        )
        airport_code: String,
        @McpToolParam(
            description = "Flight direction: 'inbound', 'outbound', or 'both' (default: 'both').",
            required = false
        )
        direction: String?,
        @McpToolParam(
            description = "Maximum number of flights to return (default 20).",
            required = false
        )
        limit: Int?
    ): AirportFlightsResponse {
        return flightradarService.getAirportFlights(
            airportCode = airport_code,
            direction = direction,
            limit = limit ?: 20
        )
    }

    @McpTool(
        name = "get_airport_details",
        description = "Retrieves detailed airport information including name, city, country, coordinates, elevation, timezone, and runways."
    )
    fun getAirportDetails(
        @McpToolParam(
            description = "IATA or ICAO airport code (e.g. AMS, EHAM, LHR, JFK).",
            required = true
        )
        airport_code: String
    ): AirportDetailsResponse {
        return flightradarService.getAirportDetails(airport_code)
    }
}
