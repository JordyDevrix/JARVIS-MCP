package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.FlightradarClient
import com.clovercloud.jarvis.config.FlightradarProperties
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlightradarServiceTest {

    private lateinit var service: FlightradarService

    @BeforeEach
    fun setUp() {
        val properties = FlightradarProperties()
        val client = FlightradarClient(properties)
        service = FlightradarService(client)
    }

    @Test
    fun `getNearbyFlights without config returns 24H datetime and validity note`() {
        val response = service.getNearbyFlights(10.0, 52.3676, 4.9041)

        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~15-30 seconds"))
    }

    @Test
    fun `getFlightDetails without config returns 24H datetime and validity note`() {
        val response = service.getFlightDetails("KL1234")

        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~30-60 seconds"))
    }

    @Test
    fun `getAirportFlights without config returns 24H datetime and validity note`() {
        val response = service.getAirportFlights("AMS")

        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for ~1-2 minutes"))
    }

    @Test
    fun `getAirportDetails without config returns 24H datetime and validity note`() {
        val response = service.getAirportDetails("AMS")

        assertNotNull(response.retrievedAt)
        assertNotNull(response.note)
        assertTrue(response.note!!.contains("valid for several months"))
    }
}
