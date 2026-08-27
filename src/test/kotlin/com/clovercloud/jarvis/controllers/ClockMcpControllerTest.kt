package com.clovercloud.jarvis.controllers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ClockMcpControllerTest {

    private val controller = ClockMcpController()

    @Test
    fun testGetCurrentTimeDefaultUtc() {
        val response = controller.getCurrentTime(null)
        assertNotNull(response)
        assertEquals("UTC", response.timeZone)
        assertNotNull(response.isoTimestamp)
        assertNotNull(response.formattedDate)
        assertNotNull(response.formattedTime)
        assertNotNull(response.dayOfWeek)
        assertNotNull(response.epochSeconds)
    }

    @Test
    fun testGetCurrentTimeCustomTimezone() {
        val response = controller.getCurrentTime("Europe/Amsterdam")
        assertNotNull(response)
        assertEquals("Europe/Amsterdam", response.timeZone)
    }

    @Test
    fun testGetCurrentTimeInvalidTimezoneFallsBackToUtc() {
        val response = controller.getCurrentTime("Invalid/Timezone")
        assertNotNull(response)
        assertEquals("UTC", response.timeZone)
    }
}
