package com.clovercloud.jarvis.clients

import com.clovercloud.jarvis.clients.dto.Fr24AirportFull
import com.clovercloud.jarvis.clients.dto.Fr24LivePositionsResponse
import com.clovercloud.jarvis.config.FlightradarProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

@Component
class FlightradarClient(
    private val properties: FlightradarProperties
) {
    private val logger = LoggerFactory.getLogger(FlightradarClient::class.java)

    private val restClient: RestClient by lazy {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds))
            setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds))
        }

        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Accept-Version", "v1")
            .defaultHeader("User-Agent", "JARVIS-MCP/1.0 (https://github.com/JordyDevrix/JARVIS)")
            .defaultRequest { spec ->
                if (properties.apiToken.isNotBlank()) {
                    spec.header("Authorization", "Bearer ${properties.apiToken.trim()}")
                }
            }
            .build()
    }

    fun isConfigured(): Boolean = properties.apiToken.isNotBlank()

    fun getLiveFlightPositions(queryParams: Map<String, String>): Fr24LivePositionsResponse? {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/api/live/flight-positions/full")
                    queryParams.forEach { (key, value) ->
                        uriBuilder.queryParam(key, value)
                    }
                    uriBuilder.build()
                }
                .retrieve()
                .body(Fr24LivePositionsResponse::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("Flightradar24 API error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to connect to Flightradar24 API: ${e.message}", e)
            null
        }
    }

    fun getAirportFull(code: String): Fr24AirportFull? {
        return try {
            restClient.get()
                .uri("/api/static/airports/{code}/full", code.trim().uppercase())
                .retrieve()
                .body(Fr24AirportFull::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("Flightradar24 airport lookup error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to lookup airport from Flightradar24 API: ${e.message}", e)
            null
        }
    }
}
