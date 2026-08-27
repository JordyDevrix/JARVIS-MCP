package com.clovercloud.jarvis.tools

import com.clovercloud.jarvis.facades.OvFacade
import com.clovercloud.jarvis.responses.ov.LiveOvLocationsResponse
import com.clovercloud.jarvis.responses.ov.OvStopDeparturesResponse
import com.clovercloud.jarvis.responses.ov.OvStopsSearchResponse
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class OvTools(
    private val ovFacade: OvFacade
) {

    @McpTool(
        name = "get_live_ov_locations",
        description = "Finds real-time public transport vehicles (buses, trams, metros) in the Netherlands near a location. Returns live GPS coordinates, distance from observer, speed, bearing, operator, line number, and trip status."
    )
    fun getLiveOvLocations(
        @McpToolParam(
            description = "Search radius in kilometers (e.g. 2.0, 5.0, 10.0). Defaults to 5.0 km.",
            required = false
        )
        radius_km: Double?,
        @McpToolParam(
            description = "Observer latitude. Defaults to current device/mock latitude (Amsterdam) if omitted.",
            required = false
        )
        latitude: Double?,
        @McpToolParam(
            description = "Observer longitude. Defaults to current device/mock longitude (Amsterdam) if omitted.",
            required = false
        )
        longitude: Double?,
        @McpToolParam(
            description = "Filter by transit operator code (e.g. GVB, RET, HTM, CXX, ARR, EBS, KEOLIS, QBUZZ).",
            required = false
        )
        operator: String?,
        @McpToolParam(
            description = "Filter by transport type: 'BUS', 'TRAM', 'METRO', or 'ALL' (default 'ALL').",
            required = false
        )
        transport_type: String?,
        @McpToolParam(
            description = "Filter by line number (e.g. '300', '26', '51', 'M357').",
            required = false
        )
        line_number: String?,
        @McpToolParam(
            description = "Maximum number of vehicles to return (default 25, max 100).",
            required = false
        )
        limit: Int?
    ): LiveOvLocationsResponse {
        return ovFacade.getLiveLocations(
            radiusKm = radius_km,
            latitude = latitude,
            longitude = longitude,
            operator = operator,
            transportType = transport_type,
            lineNumber = line_number,
            limit = limit
        )
    }

    @McpTool(
        name = "get_ov_stop_departures",
        description = "Retrieves live upcoming departures, vehicle passes, delay predictions, and platforms for any Dutch public transport stop or station. Accepts a stop area code (e.g. 'schns', '09500') or station/stop name (e.g. 'Schiphol', 'Amsterdam Centraal', 'Station Zuid')."
    )
    fun getOvStopDepartures(
        @McpToolParam(
            description = "Stop area code (e.g. 'schns', '09500', 'utrgra') OR station/stop name (e.g. 'Schiphol', 'Centraal Station').",
            required = true
        )
        stop: String,
        @McpToolParam(
            description = "Filter by transport type: 'BUS', 'TRAM', 'METRO', or null for all.",
            required = false
        )
        transport_type: String?,
        @McpToolParam(
            description = "Maximum number of departures to return (default 20, max 50).",
            required = false
        )
        limit: Int?
    ): OvStopDeparturesResponse {
        return ovFacade.getStopDepartures(
            stopAreaCodeOrQuery = stop,
            transportType = transport_type,
            limit = limit
        )
    }

    @McpTool(
        name = "search_ov_stops",
        description = "Searches for Dutch public transport stops, stations, and transit hubs by city or stop name, or finds the closest stops around given coordinates."
    )
    fun searchOvStops(
        @McpToolParam(
            description = "Search query for stop or city name (e.g. 'Schiphol', 'Centraal', 'Leiden', 'Eindhoven').",
            required = false
        )
        query: String?,
        @McpToolParam(
            description = "Observer latitude to find nearby stops.",
            required = false
        )
        latitude: Double?,
        @McpToolParam(
            description = "Observer longitude to find nearby stops.",
            required = false
        )
        longitude: Double?,
        @McpToolParam(
            description = "Search radius in kilometers if searching by coordinates (default 5.0 km).",
            required = false
        )
        radius_km: Double?,
        @McpToolParam(
            description = "Maximum number of stops to return (default 15, max 50).",
            required = false
        )
        limit: Int?
    ): OvStopsSearchResponse {
        return ovFacade.searchStops(
            query = query,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radius_km,
            limit = limit
        )
    }
}
