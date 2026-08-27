package com.clovercloud.jarvis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "location")
data class LocationProperties(
    var nominatimBaseUrl: String = "https://nominatim.openstreetmap.org",
    var ipGeolocationBaseUrl: String = "http://ip-api.com/json",
    var connectTimeoutSeconds: Long = 5,
    var readTimeoutSeconds: Long = 10,
    var cacheTtlMinutes: Long = 60,
    var defaultLatitude: Double = 52.3676,
    var defaultLongitude: Double = 4.9041,
    var defaultCity: String = "Amsterdam",
    var defaultCountry: String = "Netherlands",
    var defaultCountryCode: String = "NL"
)
