package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.OsmClient
import com.clovercloud.jarvis.config.LocationProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocationServiceTest {

    private lateinit var client: FakeOsmClient
    private lateinit var properties: LocationProperties
    private lateinit var service: LocationService

    class FakeOsmClient : OsmClient(LocationProperties()) {
        var reverseJson: String? = null
        var searchJson: String? = null
        var nearbyJson: String? = null
        var ipJson: String? = null

        override fun reverseGeocode(latitude: Double, longitude: Double, zoom: Int): String? = reverseJson
        override fun search(query: String, countryCode: String?, limit: Int): String? = searchJson
        override fun searchNearby(query: String, viewbox: String, limit: Int): String? = nearbyJson
        override fun getIpGeolocation(ip: String?): String? = ipJson
    }

    @BeforeEach
    fun setUp() {
        properties = LocationProperties()
        client = FakeOsmClient()
        service = LocationService(client, properties)
    }

    @Test
    fun `reverseGeocode parses address and sets 24H datetime and validity note`() {
        client.reverseJson = """
            {
              "place_id": 157021348,
              "osm_type": "way",
              "osm_id": 416398523,
              "lat": "52.3676280",
              "lon": "4.9041180",
              "category": "highway",
              "type": "primary",
              "display_name": "Meester Visserplein, Nieuwmarkt, Centrum, Amsterdam, Noord-Holland, 1011 RD, Nederland",
              "address": {
                "road": "Meester Visserplein",
                "suburb": "Centrum",
                "city": "Amsterdam",
                "state": "Noord-Holland",
                "country": "Nederland",
                "postcode": "1011 RD",
                "country_code": "nl"
              },
              "boundingbox": ["52.3676", "52.3678", "4.9037", "4.9041"]
            }
        """.trimIndent()

        val result = service.reverseGeocode(52.3676, 4.9041)

        assertEquals("Meester Visserplein, Nieuwmarkt, Centrum, Amsterdam, Noord-Holland, 1011 RD, Nederland", result.displayName)
        assertNotNull(result.retrievedAt)
        assertNotNull(result.note)
        assertTrue(result.note!!.contains("valid indefinitely"))
    }

    @Test
    fun `geocode searches address with 24H datetime and validity note`() {
        client.searchJson = """
            [
              {
                "place_id": 156903775,
                "lat": "52.3731162",
                "lon": "4.8923511",
                "category": "place",
                "type": "square",
                "display_name": "Dam, Centrum, Amsterdam, Noord-Holland, Nederland",
                "importance": 0.47,
                "address": {
                  "square": "Dam",
                  "city": "Amsterdam",
                  "country": "Nederland",
                  "country_code": "nl"
                }
              }
            ]
        """.trimIndent()

        val result = service.geocode("Dam Square Amsterdam", "nl", 5)

        assertEquals(1, result.totalResults)
        assertNotNull(result.retrievedAt)
        assertNotNull(result.note)
        assertTrue(result.note!!.contains("valid indefinitely"))
    }

    @Test
    fun `findNearbyPlaces includes 24H datetime and validity note`() {
        client.nearbyJson = """
            [
              {
                "place_id": 156983288,
                "lat": "52.3626107",
                "lon": "4.8986580",
                "category": "amenity",
                "type": "pharmacy",
                "name": "MedicijnMan Apotheek",
                "display_name": "MedicijnMan Apotheek, Utrechtsestraat, Amsterdam",
                "address": {
                  "road": "Utrechtsestraat",
                  "city": "Amsterdam",
                  "postcode": "1017 VR"
                }
              }
            ]
        """.trimIndent()

        val result = service.findNearbyPlaces("pharmacy", 52.3676, 4.9041, 2.0, 10)

        assertEquals(1, result.totalFound)
        assertNotNull(result.retrievedAt)
        assertNotNull(result.note)
        assertTrue(result.note!!.contains("valid for ~1-2 hours"))
    }

    @Test
    fun `getCurrentLocation includes 24H datetime and validity note`() {
        client.ipJson = """
            {
              "status": "success",
              "country": "The Netherlands",
              "countryCode": "NL",
              "regionName": "South Holland",
              "city": "The Hague",
              "zip": "2521",
              "lat": 52.0632,
              "lon": 4.3188,
              "timezone": "Europe/Amsterdam",
              "isp": "KPN B.V",
              "query": "81.207.21.18"
            }
        """.trimIndent()

        val location = service.getCurrentLocation()

        assertEquals("The Hague", location.city)
        assertNotNull(location.retrievedAt)
        assertNotNull(location.note)
        assertTrue(location.note!!.contains("valid for ~10-15 minutes"))
    }
}
