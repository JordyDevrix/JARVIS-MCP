package com.clovercloud.jarvis.clients.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24FlightPosition(
    @JsonProperty("fr24_id") val fr24Id: String? = null,
    @JsonProperty("flight") val flight: String? = null,
    @JsonProperty("callsign") val callsign: String? = null,
    @JsonProperty("lat") val lat: Double = 0.0,
    @JsonProperty("lon") val lon: Double = 0.0,
    @JsonProperty("track") val track: Int? = null,
    @JsonProperty("alt") val alt: Int? = null,
    @JsonProperty("gspeed") val gspeed: Int? = null,
    @JsonProperty("vspeed") val vspeed: Int? = null,
    @JsonProperty("squawk") val squawk: String? = null,
    @JsonProperty("timestamp") val timestamp: String? = null,
    @JsonProperty("source") val source: String? = null,
    @JsonProperty("hex") val hex: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("reg") val reg: String? = null,
    @JsonProperty("painted_as") val paintedAs: String? = null,
    @JsonProperty("operating_as") val operatingAs: String? = null,
    @JsonProperty("orig_iata") val origIata: String? = null,
    @JsonProperty("orig_icao") val origIcao: String? = null,
    @JsonProperty("dest_iata") val destIata: String? = null,
    @JsonProperty("dest_icao") val destIcao: String? = null,
    @JsonProperty("eta") val eta: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fr24LivePositionsResponse(
    @JsonProperty("data") val data: List<Fr24FlightPosition> = emptyList()
)
