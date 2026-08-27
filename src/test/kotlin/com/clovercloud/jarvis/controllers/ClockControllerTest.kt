package com.clovercloud.jarvis.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ClockControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = ClockController()
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET v1 tools clock time returns 200 with dateTime24h and note`() {
        mockMvc.perform(get("/v1/tools/clock/time?timezone=Europe/Amsterdam"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timeZone").value("Europe/Amsterdam"))
            .andExpect(jsonPath("$.dateTime24h").isNotEmpty)
            .andExpect(jsonPath("$.note").isNotEmpty)
    }
}
