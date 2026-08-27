package com.clovercloud.jarvis.clients

import com.clovercloud.jarvis.config.LocationProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

@Component
class OsmClient(
    private val properties: LocationProperties
) {
    private val logger = LoggerFactory.getLogger(OsmClient::class.java)

    private val restClient: RestClient by lazy {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds))
            setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds))
        }

        RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "JARVIS-MCP/1.0 (https://github.com/JordyDevrix/JARVIS; pair-programming-agent)")
            .defaultHeader("Accept", "application/json")
            .build()
    }

    fun reverseGeocode(latitude: Double, longitude: Double, zoom: Int = 18): String? {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/reverse")
                        .queryParam("lat", latitude.toString())
                        .queryParam("lon", longitude.toString())
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", "1")
                        .queryParam("zoom", zoom.toString())
                        .build()
                }
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("Nominatim reverse geocode error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to reverse geocode ($latitude, $longitude): ${e.message}", e)
            null
        }
    }

    fun search(query: String, countryCode: String? = null, limit: Int = 5): String? {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q", query.trim())
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", "1")
                        .queryParam("limit", limit.toString())
                    if (!countryCode.isNullOrBlank()) {
                        uriBuilder.queryParam("countrycodes", countryCode.trim().lowercase())
                    }
                    uriBuilder.build()
                }
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("Nominatim search error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to search location '$query': ${e.message}", e)
            null
        }
    }

    fun searchNearby(query: String, viewbox: String, limit: Int = 10): String? {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q", query.trim())
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", "1")
                        .queryParam("bounded", "1")
                        .queryParam("viewbox", viewbox)
                        .queryParam("limit", limit.toString())
                        .build()
                }
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("Nominatim nearby search error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to search nearby '$query' in viewbox $viewbox: ${e.message}", e)
            null
        }
    }

    fun getIpGeolocation(ip: String? = null): String? {
        return try {
            val targetUrl = if (ip.isNullOrBlank()) {
                properties.ipGeolocationBaseUrl.trimEnd('/')
            } else {
                "${properties.ipGeolocationBaseUrl.trimEnd('/')}/${ip.trim()}"
            }
            restClient.get()
                .uri(targetUrl)
                .retrieve()
                .body(String::class.java)
        } catch (e: RestClientResponseException) {
            logger.warn("IP Geolocation error [${e.statusCode}]: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("Failed to fetch IP geolocation: ${e.message}", e)
            null
        }
    }
}
