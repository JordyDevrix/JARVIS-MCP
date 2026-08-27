package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.facades.OvFacade
import com.clovercloud.jarvis.requests.ov.LiveOvLocationsRequest
import com.clovercloud.jarvis.requests.ov.OvStopDeparturesRequest
import com.clovercloud.jarvis.requests.ov.SearchOvStopsRequest
import com.clovercloud.jarvis.responses.ov.LiveOvLocationsResponse
import com.clovercloud.jarvis.responses.ov.OvStopDeparturesResponse
import com.clovercloud.jarvis.responses.ov.OvStopsSearchResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
@Tag(name = "Dutch OV Tools", description = "Real-time Dutch public transport information (buses, trams, metros, departures, stops)")
class OvController(
    private val ovFacade: OvFacade
) {

    @Operation(summary = "Get live public transport vehicle locations", description = "Returns real-time GPS locations of buses, trams, and metros near an observer location.")
    @GetMapping("/v1/tools/ov/locations")
    fun getLiveLocations(
        @Parameter(description = "Search radius in km", example = "5.0")
        @RequestParam(required = false, defaultValue = "5.0") radius_km: Double,
        @Parameter(description = "Observer latitude (defaults to mock/current location)")
        @RequestParam(required = false) latitude: Double?,
        @Parameter(description = "Observer longitude (defaults to mock/current location)")
        @RequestParam(required = false) longitude: Double?,
        @Parameter(description = "Filter by transit operator (e.g. GVB, RET, HTM, CXX, ARR, EBS, KEOLIS, QBUZZ)", example = "GVB")
        @RequestParam(required = false) `operator`: String?,
        @Parameter(description = "Filter by transport type: BUS, TRAM, METRO, or ALL", example = "ALL")
        @RequestParam(required = false) transport_type: String?,
        @Parameter(description = "Filter by line number", example = "300")
        @RequestParam(required = false) line_number: String?,
        @Parameter(description = "Maximum results to return", example = "25")
        @RequestParam(required = false, defaultValue = "25") limit: Int
    ): ResponseEntity<LiveOvLocationsResponse> {
        val result = ovFacade.getLiveLocations(
            radiusKm = radius_km,
            latitude = latitude,
            longitude = longitude,
            operator = operator,
            transportType = transport_type,
            lineNumber = line_number,
            limit = limit
        )
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "Get live departures for a stop or station", description = "Returns live departure board, delays, platforms, and passes for a stop code or station name.")
    @GetMapping("/v1/tools/ov/departures")
    fun getStopDepartures(
        @Parameter(description = "Stop area code (e.g. 'schns') or station name (e.g. 'Schiphol', 'Amsterdam Centraal')", example = "schns", required = true)
        @RequestParam stop: String,
        @Parameter(description = "Filter by transport type (BUS, TRAM, METRO)", example = "BUS")
        @RequestParam(required = false) transport_type: String?,
        @Parameter(description = "Maximum departures to return", example = "20")
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): ResponseEntity<OvStopDeparturesResponse> {
        val result = ovFacade.getStopDepartures(
            stopAreaCodeOrQuery = stop,
            transportType = transport_type,
            limit = limit
        )
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "Search Dutch public transport stops", description = "Search ~4,450 transit stops and stations by city/name or geographic coordinates.")
    @GetMapping("/v1/tools/ov/stops")
    fun searchStops(
        @Parameter(description = "Search query for stop or city name", example = "Schiphol")
        @RequestParam(required = false) query: String?,
        @Parameter(description = "Observer latitude to find nearby stops")
        @RequestParam(required = false) latitude: Double?,
        @Parameter(description = "Observer longitude to find nearby stops")
        @RequestParam(required = false) longitude: Double?,
        @Parameter(description = "Search radius in km if searching by coordinates", example = "5.0")
        @RequestParam(required = false, defaultValue = "5.0") radius_km: Double,
        @Parameter(description = "Maximum results", example = "15")
        @RequestParam(required = false, defaultValue = "15") limit: Int
    ): ResponseEntity<OvStopsSearchResponse> {
        val result = ovFacade.searchStops(
            query = query,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radius_km,
            limit = limit
        )
        return ResponseEntity.ok(result)
    }
}
