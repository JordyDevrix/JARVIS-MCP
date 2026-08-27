package com.clovercloud.jarvis.controllers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class RestControllersTest {

    private val clockController = ClockController()
    private val locationController = LocationController()
    private val devController = DevelopmentController()

    @Test
    fun testClockController() {
        val response = clockController.getCurrentTime("Europe/Amsterdam")
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Europe/Amsterdam", response.body?.timeZone)
    }

    @Test
    fun testLocationControllerCurrent() {
        val response = locationController.getCurrentLocation()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Amsterdam", response.body?.city)
    }

    @Test
    fun testLocationControllerLookup() {
        val response = locationController.lookupLocation("London")
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("London", response.body?.city)
        assertEquals("United Kingdom", response.body?.country)
    }

    @Test
    fun testDevelopmentControllerHealth() {
        val response = devController.health()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Healthy", response.body)
    }
}
