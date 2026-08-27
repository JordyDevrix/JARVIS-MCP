package com.clovercloud.jarvis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "flightradar24")
data class FlightradarProperties(
    var apiToken: String = "",
    var baseUrl: String = "https://fr24api.flightradar24.com",
    var connectTimeoutSeconds: Long = 3,
    var readTimeoutSeconds: Long = 5
)
