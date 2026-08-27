package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.OvClient
import com.clovercloud.jarvis.config.OvProperties
import com.google.transit.realtime.GtfsRealtime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class OvServiceTest {

    private lateinit var client: FakeOvClient
    private lateinit var properties: OvProperties
    private lateinit var service: OvService

    class FakeOvClient : OvClient(OvProperties()) {
        var vehiclePbBytes: ByteArray? = null
        var stopDeparturesJson: String? = null
        var allStopAreasJson: String? = null

        override fun getVehiclePositionsPb(): ByteArray? = vehiclePbBytes
        override fun getStopAreaDepartures(stopAreaCode: String): String? = stopDeparturesJson
        override fun getAllStopAreas(): String? = allStopAreasJson
    }

    @BeforeEach
    fun setUp() {
        properties = OvProperties()
        client = FakeOvClient()
        service = OvService(client, properties)
    }

    @Test
    fun `getLiveVehicleLocations includes 24H datetime and estimated validity note`() {
        val feed = GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(
                GtfsRealtime.FeedHeader.newBuilder()
                    .setGtfsRealtimeVersion("2.0")
                    .setTimestamp(System.currentTimeMillis() / 1000)
            )
            .addEntity(
                GtfsRealtime.FeedEntity.newBuilder()
                    .setId("2026-08-28:GVB:26:1001")
                    .setVehicle(
                        GtfsRealtime.VehiclePosition.newBuilder()
                            .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setLabel("2001").build())
                            .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("T101").setRouteId("R26").build())
                            .setPosition(
                                GtfsRealtime.Position.newBuilder()
                                    .setLatitude(52.3700f)
                                    .setLongitude(4.9000f)
                                    .setSpeed(10.0f)
                                    .setBearing(90.0f)
                                    .build()
                            )
                            .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO)
                            .build()
                    )
            )
            .build()

        val outputStream = ByteArrayOutputStream()
        feed.writeTo(outputStream)
        client.vehiclePbBytes = outputStream.toByteArray()

        val response = service.getLiveVehicleLocations(
            radiusKm = 10.0,
            observerLat = 52.3676,
            observerLon = 4.9041,
            limit = 25
        )

        assertEquals(1, response.vehiclesFoundCount)
        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~15-30 seconds"))
    }

    @Test
    fun `getStopDepartures includes 24H datetime and estimated validity note`() {
        client.stopDeparturesJson = """
            {
              "schns": {
                "57330760": {
                  "Stop": {
                    "TimingPointName": "Schiphol, Airport",
                    "TimingPointTown": "Schiphol",
                    "Latitude": 52.309586,
                    "Longitude": 4.760094
                  },
                  "GeneralMessages": {},
                  "Passes": {
                    "P1": {
                      "TransportType": "BUS",
                      "LinePublicNumber": "300",
                      "LineName": "Haarlem - Amsterdam Bijlmer ArenA",
                      "DestinationName50": "Amsterdam Bijlmer ArenA",
                      "DataOwnerCode": "CXX",
                      "TargetDepartureTime": "2026-08-28T00:24:00+02:00",
                      "ExpectedDepartureTime": "2026-08-28T00:27:00+02:00",
                      "SideCode": "B19",
                      "TripStopStatus": "DRIVING",
                      "WheelChairAccessible": "ACCESSIBLE",
                      "JourneyNumber": "311"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = service.getStopDepartures("schns")

        assertEquals("schns", response.stopAreaCode)
        assertEquals(1, response.totalDepartures)
        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~30-60 seconds"))
    }

    @Test
    fun `searchStops includes 24H datetime and estimated validity note`() {
        client.allStopAreasJson = """
            {
              "schns": {
                "StopAreaCode": "schns",
                "TimingPointName": "Schiphol, Airport",
                "TimingPointTown": "Schiphol",
                "Latitude": 52.3095,
                "Longitude": 4.7600
              }
            }
        """.trimIndent()

        val response = service.searchStops(query = "Schiphol")

        assertEquals(1, response.totalMatches)
        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~24 hours"))
    }
}
