package com.clovercloud.jarvis.clients.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24Country(
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("name") val name: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24Timezone(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("offset") val offset: Any? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24Runway(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("designator") val designator: String? = null,
    @JsonProperty("heading") val heading: Int? = null,
    @JsonProperty("length") val length: Double? = null,
    @JsonProperty("width") val width: Double? = null,
    @JsonProperty("surface") val surface: Any? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24AirportFull(
    @JsonProperty("name") val name: String = "",
    @JsonProperty("iata") val iata: String? = null,
    @JsonProperty("icao") val icao: String? = null,
    @JsonProperty("lat") val lat: Double = 0.0,
    @JsonProperty("lon") val lon: Double = 0.0,
    @JsonProperty("elevation") val elevation: Double? = null,
    @JsonProperty("city") val city: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("country") val country: Fr24Country? = null,
    @JsonProperty("timezone") val timezone: Fr24Timezone? = null,
    @JsonProperty("runways") val runways: List<Fr24Runway> = emptyList()
)
