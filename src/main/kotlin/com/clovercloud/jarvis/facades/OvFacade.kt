package com.clovercloud.jarvis.facades

import com.clovercloud.jarvis.responses.ov.LiveOvLocationsResponse
import com.clovercloud.jarvis.responses.ov.OvStopDeparturesResponse
import com.clovercloud.jarvis.responses.ov.OvStopsSearchResponse
import com.clovercloud.jarvis.services.OvService
import org.springframework.stereotype.Component

/**
 * Facade providing a unified entry point to the Dutch Public Transport (OV) domain.
 * Decouples controllers and MCP tool handlers from underlying services and data clients.
 */
@Component
class OvFacade(
    private val ovService: OvService,
    private val locationFacade: LocationFacade
) {

    fun getLiveLocations(
        radiusKm: Double?,
        latitude: Double?,
        longitude: Double?,
        operator: String?,
        transportType: String?,
        lineNumber: String?,
        limit: Int?
    ): LiveOvLocationsResponse {
        val radius = radiusKm ?: 5.0
        val currentLoc = if (latitude == null || longitude == null) locationFacade.getCurrentLocation() else null
        val lat = latitude ?: currentLoc?.latitude ?: 52.3676
        val lon = longitude ?: currentLoc?.longitude ?: 4.9041
        val maxLimit = limit ?: 25

        return ovService.getLiveVehicleLocations(
            radiusKm = radius,
            observerLat = lat,
            observerLon = lon,
            operator = operator,
            transportType = transportType,
            lineNumber = lineNumber,
            limit = maxLimit
        )
    }

    fun getStopDepartures(
        stopAreaCodeOrQuery: String,
        transportType: String?,
        limit: Int?
    ): OvStopDeparturesResponse {
        return ovService.getStopDepartures(
            stopAreaCodeOrQuery = stopAreaCodeOrQuery,
            transportType = transportType,
            limit = limit ?: 20
        )
    }

    fun searchStops(
        query: String?,
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double?,
        limit: Int?
    ): OvStopsSearchResponse {
        return ovService.searchStops(
            query = query,
            observerLat = latitude,
            observerLon = longitude,
            radiusKm = radiusKm ?: 5.0,
            limit = limit ?: 15
        )
    }
}
