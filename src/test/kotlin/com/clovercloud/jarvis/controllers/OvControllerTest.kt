package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.facades.OvFacade
import com.clovercloud.jarvis.responses.ov.LiveOvLocationsResponse
import com.clovercloud.jarvis.responses.ov.OvDepartureItem
import com.clovercloud.jarvis.responses.ov.OvStopDeparturesResponse
import com.clovercloud.jarvis.responses.ov.OvStopItem
import com.clovercloud.jarvis.responses.ov.OvStopsSearchResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class OvControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var fakeFacade: FakeOvFacade

    class FakeOvFacade : OvFacade(
        ovService = com.clovercloud.jarvis.services.OvService(
            com.clovercloud.jarvis.clients.OvClient(com.clovercloud.jarvis.config.OvProperties()),
            com.clovercloud.jarvis.config.OvProperties()
        ),
        locationFacade = com.clovercloud.jarvis.facades.LocationFacade(
            com.clovercloud.jarvis.services.LocationService(
                com.clovercloud.jarvis.clients.OsmClient(com.clovercloud.jarvis.config.LocationProperties()),
                com.clovercloud.jarvis.config.LocationProperties()
            )
        )
    ) {
        override fun getLiveLocations(
            radiusKm: Double?,
            latitude: Double?,
            longitude: Double?,
            operator: String?,
            transportType: String?,
            lineNumber: String?,
            limit: Int?
        ): LiveOvLocationsResponse {
            return LiveOvLocationsResponse(
                observerLocation = "Lat: 52.3676, Lon: 4.9041",
                searchRadiusKm = radiusKm ?: 5.0,
                vehiclesFoundCount = 0,
                vehicles = emptyList(),
                retrievedAt = "2026-08-28 00:30:00",
                note = "Live vehicle GPS coordinates are real-time telemetry and estimated to be valid for ~15-30 seconds."
            )
        }

        override fun getStopDepartures(
            stopAreaCodeOrQuery: String,
            transportType: String?,
            limit: Int?
        ): OvStopDeparturesResponse {
            return OvStopDeparturesResponse(
                stopAreaCode = stopAreaCodeOrQuery,
                stopName = "Schiphol, Airport",
                town = "Schiphol",
                latitude = 52.3095,
                longitude = 4.7600,
                totalDepartures = 1,
                departures = listOf(
                    OvDepartureItem(
                        linePublicNumber = "300",
                        lineName = "Haarlem - Amsterdam Bijlmer ArenA",
                        destination = "Amsterdam Bijlmer ArenA",
                        transportType = "BUS",
                        operatorCode = "CXX",
                        targetDepartureTime = "2026-08-28T00:24:00+02:00",
                        expectedDepartureTime = "2026-08-28T00:26:00+02:00",
                        delayMinutes = 2,
                        platform = "B19",
                        tripStopStatus = "DRIVING",
                        wheelChairAccessible = "ACCESSIBLE",
                        journeyNumber = "311"
                    )
                ),
                retrievedAt = "2026-08-28 00:30:00",
                note = "Live departure times, platform assignments, and delay predictions are estimated to be valid for ~30-60 seconds."
            )
        }

        override fun searchStops(
            query: String?,
            latitude: Double?,
            longitude: Double?,
            radiusKm: Double?,
            limit: Int?
        ): OvStopsSearchResponse {
            return OvStopsSearchResponse(
                query = query,
                totalMatches = 1,
                stops = listOf(
                    OvStopItem(
                        stopAreaCode = "schns",
                        name = "Schiphol, Airport",
                        town = "Schiphol",
                        latitude = 52.3095,
                        longitude = 4.7600
                    )
                ),
                retrievedAt = "2026-08-28 00:30:00",
                note = "Public transport stop and station data is estimated to be valid for ~24 hours."
            )
        }
    }

    @BeforeEach
    fun setUp() {
        fakeFacade = FakeOvFacade()
        val controller = OvController(fakeFacade)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET v1 tools ov locations returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/ov/locations?radius_km=5.0"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.searchRadiusKm").value(5.0))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `GET v1 tools ov departures returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/ov/departures?stop=schns"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.stopAreaCode").value("schns"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `GET v1 tools ov stops returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/ov/stops?query=Schiphol"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMatches").value(1))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `POST v1 mcp ov departures returns 200 with retrievedAt and note`() {
        mockMvc.perform(
            post("/v1/mcp/ov/departures")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"stopAreaCode":"schns"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.stopAreaCode").value("schns"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }
}
