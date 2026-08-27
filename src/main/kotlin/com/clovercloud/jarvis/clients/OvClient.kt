package com.clovercloud.jarvis.clients

import com.clovercloud.jarvis.config.OvProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

@Component
class OvClient(
    private val properties: OvProperties
) {
    private val logger = LoggerFactory.getLogger(OvClient::class.java)

    private val restClient: RestClient by lazy {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds))
            setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds))
        }

        RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "JARVIS-MCP/1.0 (https://github.com/JordyDevrix/JARVIS; contact: secretariaat@openov.nl)")
            .build()
    }

    fun getVehiclePositionsPb(): ByteArray? {
        return try {
            val url = "${properties.gtfsBaseUrl.trimEnd('/')}/vehiclePositions.pb"
            restClient.get()
                .uri(url)
                .header("Accept", "application/x-protobuf, application/octet-stream, */*")
                .retrieve()
                .body(ByteArray::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("OVapi GTFS-RT error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to fetch GTFS-RT vehicle positions: ${e.message}", e)
            null
        }
    }

    fun getStopAreaDepartures(stopAreaCode: String): String? {
        return try {
            val url = "${properties.baseUrl.trimEnd('/')}/stopareacode/{code}/departures"
            restClient.get()
                .uri(url, stopAreaCode.trim())
                .header("Accept", "application/json")
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("OVapi stop departure error for '$stopAreaCode' [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to fetch stop departures for '$stopAreaCode': ${e.message}", e)
            null
        }
    }

    fun getAllStopAreas(): String? {
        return try {
            val url = "${properties.baseUrl.trimEnd('/')}/stopareacode/"
            restClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("OVapi stoparea list error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to fetch all stop areas from OVapi: ${e.message}", e)
            null
        }
    }
}
