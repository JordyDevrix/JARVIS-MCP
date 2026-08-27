package com.clovercloud.jarvis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ovapi")
data class OvProperties(
    var baseUrl: String = "http://v0.ovapi.nl",
    var gtfsBaseUrl: String = "https://gtfs.ovapi.nl/nl",
    var connectTimeoutSeconds: Long = 5,
    var readTimeoutSeconds: Long = 10,
    var vehicleCacheTtlSeconds: Long = 15,
    var stopCacheTtlHours: Long = 24
)
