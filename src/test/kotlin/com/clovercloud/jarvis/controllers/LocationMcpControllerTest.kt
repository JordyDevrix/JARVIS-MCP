package com.clovercloud.jarvis.controllers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LocationMcpControllerTest {

    private val controller = LocationMcpController()

    @Test
    fun testGetCurrentLocation() {
        val location = controller.getCurrentLocation()
        assertNotNull(location)
        assertEquals("Amsterdam", location.city)
        assertEquals("Netherlands", location.country)
        assertEquals("NL", location.countryCode)
        assertEquals(52.3676, location.latitude)
        assertEquals(4.9041, location.longitude)
    }

    @Test
    fun testLookupLocationPreset() {
        val location = controller.lookupLocation("Tokyo")
        assertNotNull(location)
        assertEquals("Tokyo", location.city)
        assertEquals("Japan", location.country)
        assertEquals("JP", location.countryCode)
    }

    @Test
    fun testLookupLocationDefault() {
        val location = controller.lookupLocation(null)
        assertNotNull(location)
        assertEquals("Amsterdam", location.city)
    }

    @Test
    fun testLookupLocationUnknownCity() {
        val location = controller.lookupLocation("Berlin")
        assertNotNull(location)
        assertEquals("Berlin", location.city)
        assertEquals("Unknown", location.country)
        assertEquals("N/A", location.countryCode)
    }
}
