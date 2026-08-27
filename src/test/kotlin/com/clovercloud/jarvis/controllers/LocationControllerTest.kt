package com.clovercloud.jarvis.controllers

import com.clovercloud.jarvis.facades.LocationFacade
import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.responses.location.GeocodePlaceItem
import com.clovercloud.jarvis.responses.location.GeocodeResponse
import com.clovercloud.jarvis.responses.location.NearbyPlaceItem
import com.clovercloud.jarvis.responses.location.NearbyPlacesResponse
import com.clovercloud.jarvis.responses.location.ReverseGeocodeResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class LocationControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var fakeFacade: FakeLocationFacade

    class FakeLocationFacade : LocationFacade(
        locationService = com.clovercloud.jarvis.services.LocationService(
            com.clovercloud.jarvis.clients.OsmClient(com.clovercloud.jarvis.config.LocationProperties()),
            com.clovercloud.jarvis.config.LocationProperties()
        )
    ) {
        override fun getCurrentLocation(ip: String?): LocationResponse {
            return LocationResponse(
                city = "Amsterdam",
                country = "Netherlands",
                countryCode = "NL",
                latitude = 52.3676,
                longitude = 4.9041,
                timezone = "Europe/Amsterdam",
                description = "Amsterdam Headquarters",
                region = "North Holland",
                postalCode = "1011 RD",
                source = "REAL",
                retrievedAt = "2026-08-28 00:30:00",
                note = "Location snapshot is estimated to be valid for ~10-15 minutes or until the device moves or network changes."
            )
        }

        override fun reverseGeocode(latitude: Double, longitude: Double, zoom: Int?): ReverseGeocodeResponse {
            return ReverseGeocodeResponse(
                displayName = "Dam Square, Amsterdam, Netherlands",
                latitude = latitude,
                longitude = longitude,
                retrievedAt = "2026-08-28 00:30:00",
                note = "Reverse geocoded address is static and estimated to be valid indefinitely."
            )
        }

        override fun geocode(query: String, countryCode: String?, limit: Int?): GeocodeResponse {
            return GeocodeResponse(
                query = query,
                totalResults = 1,
                places = listOf(
                    GeocodePlaceItem(
                        displayName = "Dam Square, Amsterdam, Netherlands",
                        latitude = 52.3731,
                        longitude = 4.8923
                    )
                ),
                retrievedAt = "2026-08-28 00:30:00",
                note = "Geocoded coordinates and address data are static and estimated to be valid indefinitely."
            )
        }

        override fun findNearbyPlaces(
            categoryOrQuery: String,
            latitude: Double?,
            longitude: Double?,
            radiusKm: Double?,
            limit: Int?
        ): NearbyPlacesResponse {
            return NearbyPlacesResponse(
                observerLocation = "Lat: 52.3676, Lon: 4.9041",
                searchCategory = categoryOrQuery,
                searchRadiusKm = radiusKm ?: 1.0,
                totalFound = 1,
                places = listOf(
                    NearbyPlaceItem(
                        name = "MedicijnMan Apotheek",
                        category = "amenity",
                        type = "pharmacy",
                        displayName = "MedicijnMan Apotheek",
                        latitude = 52.3626,
                        longitude = 4.8986,
                        distanceMeters = 600.0,
                        distanceKm = 0.6,
                        bearingFromObserver = "SW"
                    )
                ),
                retrievedAt = "2026-08-28 00:30:00",
                note = "Nearby amenities and points of interest are estimated to be valid for ~1-2 hours."
            )
        }
    }

    @BeforeEach
    fun setUp() {
        fakeFacade = FakeLocationFacade()
        val controller = LocationController(fakeFacade)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET v1 tools location current returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/location/current"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.city").value("Amsterdam"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `GET v1 tools location reverse returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/location/reverse?latitude=52.3731&longitude=4.8923"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Dam Square, Amsterdam, Netherlands"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `GET v1 tools location geocode returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/location/geocode?query=Dam Square Amsterdam"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.query").value("Dam Square Amsterdam"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `GET v1 tools location nearby returns 200 with retrievedAt and note`() {
        mockMvc.perform(get("/v1/tools/location/nearby?query=pharmacy"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.searchCategory").value("pharmacy"))
            .andExpect(jsonPath("$.retrievedAt").value("2026-08-28 00:30:00"))
            .andExpect(jsonPath("$.note").isNotEmpty)
    }

    @Test
    fun `POST v1 mcp location reverse returns 200`() {
        mockMvc.perform(
            post("/v1/mcp/location/reverse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"latitude":52.3731,"longitude":4.8923}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Dam Square, Amsterdam, Netherlands"))
    }

    @Test
    fun `POST v1 mcp location geocode returns 200`() {
        mockMvc.perform(
            post("/v1/mcp/location/geocode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"query":"Dam Square Amsterdam"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.places[0].latitude").value(52.3731))
    }

    @Test
    fun `POST v1 mcp location nearby returns 200`() {
        mockMvc.perform(
            post("/v1/mcp/location/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryOrQuery":"pharmacy"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.places[0].name").value("MedicijnMan Apotheek"))
    }
}
