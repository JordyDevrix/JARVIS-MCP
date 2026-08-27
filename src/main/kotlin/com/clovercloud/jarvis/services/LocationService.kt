package com.clovercloud.jarvis.services

import com.clovercloud.jarvis.clients.OsmClient
import com.clovercloud.jarvis.config.LocationProperties
import com.clovercloud.jarvis.responses.LocationResponse
import com.clovercloud.jarvis.responses.location.GeocodePlaceItem
import com.clovercloud.jarvis.responses.location.GeocodeResponse
import com.clovercloud.jarvis.responses.location.NearbyPlaceItem
import com.clovercloud.jarvis.responses.location.NearbyPlacesResponse
import com.clovercloud.jarvis.responses.location.OsmAddress
import com.clovercloud.jarvis.responses.location.ReverseGeocodeResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class LocationService(
    private val client: OsmClient,
    private val properties: LocationProperties
) {
    private val logger = LoggerFactory.getLogger(LocationService::class.java)
    private val objectMapper = ObjectMapper()

    private data class CacheEntry<T>(val data: T, val timestamp: Long)

    private val reverseGeocodeCache = ConcurrentHashMap<String, CacheEntry<ReverseGeocodeResponse>>()
    private val geocodeCache = ConcurrentHashMap<String, CacheEntry<GeocodeResponse>>()
    private val nearbyPlacesCache = ConcurrentHashMap<String, CacheEntry<NearbyPlacesResponse>>()
    @Volatile private var cachedCurrentLocation: CacheEntry<LocationResponse>? = null

    fun reverseGeocode(latitude: Double, longitude: Double, zoom: Int? = 18): ReverseGeocodeResponse {
        val cleanZoom = (zoom ?: 18).coerceIn(3, 18)
        val cacheKey = "rev:%.5f:%.5f:%d".format(Locale.US, latitude, longitude, cleanZoom)

        val now = System.currentTimeMillis()
        val ttlMs = properties.cacheTtlMinutes * 60_000L

        reverseGeocodeCache[cacheKey]?.let {
            if ((now - it.timestamp) < ttlMs) return it.data
        }

        val rawJson = client.reverseGeocode(latitude, longitude, cleanZoom)
        if (rawJson.isNullOrBlank()) {
            return ReverseGeocodeResponse(
                displayName = "Location at Lat: %.5f, Lon: %.5f".format(Locale.US, latitude, longitude),
                latitude = latitude,
                longitude = longitude,
                note = "Could not retrieve reverse geocode data from OpenStreetMap."
            )
        }

        try {
            val root = objectMapper.readTree(rawJson)
            val displayName = root.path("display_name").asText("Lat: $latitude, Lon: $longitude")
            val lat = root.path("lat").asText(latitude.toString()).toDoubleOrNull() ?: latitude
            val lon = root.path("lon").asText(longitude.toString()).toDoubleOrNull() ?: longitude
            val category = root.path("category").asText(null)
            val type = root.path("type").asText(null)
            val placeId = root.path("place_id").asLong(0).takeIf { it != 0L }
            val osmType = root.path("osm_type").asText(null)
            val osmId = root.path("osm_id").asLong(0).takeIf { it != 0L }

            val addressNode = root.path("address")
            val address = if (addressNode.isObject) parseAddress(addressNode) else null

            val boundingBox = parseBoundingBox(root.path("boundingbox"))

            val response = ReverseGeocodeResponse(
                displayName = displayName,
                latitude = lat,
                longitude = lon,
                category = category,
                type = type,
                address = address,
                osmType = osmType,
                osmId = osmId,
                placeId = placeId,
                boundingBox = boundingBox
            )

            reverseGeocodeCache[cacheKey] = CacheEntry(response, now)
            return response
        } catch (e: Exception) {
            logger.error("Failed to parse Nominatim reverse geocode response: ${e.message}", e)
            return ReverseGeocodeResponse(
                displayName = "Location at Lat: %.5f, Lon: %.5f".format(Locale.US, latitude, longitude),
                latitude = latitude,
                longitude = longitude,
                note = "Error parsing reverse geocode data from OpenStreetMap."
            )
        }
    }

    fun geocode(query: String, countryCode: String? = null, limit: Int? = 5): GeocodeResponse {
        val cleanQuery = query.trim()
        val cleanLimit = (limit ?: 5).coerceIn(1, 20)
        val cleanCountry = countryCode?.trim()?.lowercase()

        val cacheKey = "geo:$cleanQuery:$cleanCountry:$cleanLimit"
        val now = System.currentTimeMillis()
        val ttlMs = properties.cacheTtlMinutes * 60_000L

        geocodeCache[cacheKey]?.let {
            if ((now - it.timestamp) < ttlMs) return it.data
        }

        val rawJson = client.search(cleanQuery, cleanCountry, cleanLimit)
        if (rawJson.isNullOrBlank()) {
            return GeocodeResponse(
                query = cleanQuery,
                totalResults = 0,
                places = emptyList(),
                note = "Could not retrieve geocoding results from OpenStreetMap."
            )
        }

        try {
            val root = objectMapper.readTree(rawJson)
            val places = mutableListOf<GeocodePlaceItem>()

            if (root.isArray) {
                for (itemNode in root) {
                    val displayName = itemNode.path("display_name").asText("")
                    val lat = itemNode.path("lat").asText("0.0").toDoubleOrNull() ?: 0.0
                    val lon = itemNode.path("lon").asText("0.0").toDoubleOrNull() ?: 0.0
                    val type = itemNode.path("type").asText(null)
                    val category = itemNode.path("category").asText(null)
                    val importance = itemNode.path("importance").asDouble(0.0).takeIf { it != 0.0 }
                    val address = if (itemNode.path("address").isObject) parseAddress(itemNode.path("address")) else null
                    val boundingBox = parseBoundingBox(itemNode.path("boundingbox"))

                    places.add(
                        GeocodePlaceItem(
                            displayName = displayName,
                            latitude = lat,
                            longitude = lon,
                            type = type,
                            category = category,
                            importance = importance,
                            address = address,
                            boundingBox = boundingBox
                        )
                    )
                }
            }

            val response = GeocodeResponse(
                query = cleanQuery,
                totalResults = places.size,
                places = places,
                note = if (places.isEmpty()) "No locations found matching '$cleanQuery'." else null
            )

            geocodeCache[cacheKey] = CacheEntry(response, now)
            return response
        } catch (e: Exception) {
            logger.error("Failed to parse Nominatim search response: ${e.message}", e)
            return GeocodeResponse(
                query = cleanQuery,
                totalResults = 0,
                places = emptyList(),
                note = "Error parsing geocoding results from OpenStreetMap."
            )
        }
    }

    fun findNearbyPlaces(
        categoryOrQuery: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double? = 1.0,
        limit: Int? = 10
    ): NearbyPlacesResponse {
        val cleanRadius = (radiusKm ?: 1.0).coerceIn(0.1, 25.0)
        val cleanLimit = (limit ?: 10).coerceIn(1, 30)
        val cleanCategory = normalizeCategory(categoryOrQuery.trim())
        val observerDesc = "Lat: %.4f, Lon: %.4f".format(Locale.US, latitude, longitude)

        val cacheKey = "nearby:$cleanCategory:%.4f:%.4f:%.1f:%d".format(Locale.US, latitude, longitude, cleanRadius, cleanLimit)
        val now = System.currentTimeMillis()
        val ttlMs = properties.cacheTtlMinutes * 60_000L

        nearbyPlacesCache[cacheKey]?.let {
            if ((now - it.timestamp) < ttlMs) return it.data
        }

        // Calculate bounding box for Nominatim viewbox: minLon,maxLat,maxLon,minLat
        val deltaLat = cleanRadius / 111.0
        val cosLat = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
        val deltaLon = cleanRadius / (111.0 * cosLat)

        val minLat = latitude - deltaLat
        val maxLat = latitude + deltaLat
        val minLon = longitude - deltaLon
        val maxLon = longitude + deltaLon

        val viewbox = "%.5f,%.5f,%.5f,%.5f".format(Locale.US, minLon, maxLat, maxLon, minLat)

        val rawJson = client.searchNearby(cleanCategory, viewbox, cleanLimit * 2)
        if (rawJson.isNullOrBlank()) {
            return NearbyPlacesResponse(
                observerLocation = observerDesc,
                searchCategory = cleanCategory,
                searchRadiusKm = cleanRadius,
                totalFound = 0,
                places = emptyList(),
                note = "Could not retrieve nearby places from OpenStreetMap."
            )
        }

        try {
            val root = objectMapper.readTree(rawJson)
            val places = mutableListOf<NearbyPlaceItem>()

            if (root.isArray) {
                for (itemNode in root) {
                    val lat = itemNode.path("lat").asText("0.0").toDoubleOrNull() ?: 0.0
                    val lon = itemNode.path("lon").asText("0.0").toDoubleOrNull() ?: 0.0
                    val distKm = haversineDistanceKm(latitude, longitude, lat, lon)

                    if (distKm <= cleanRadius) {
                        val name = itemNode.path("name").asText(null)
                            ?: itemNode.path("address").path("amenity").asText(null)
                            ?: itemNode.path("display_name").asText("Unnamed $cleanCategory")
                                .substringBefore(",")
                        val category = itemNode.path("category").asText("amenity")
                        val type = itemNode.path("type").asText(cleanCategory)
                        val displayName = itemNode.path("display_name").asText(name)

                        val addressNode = itemNode.path("address")
                        val road = addressNode.path("road").asText(null)
                        val houseNumber = addressNode.path("house_number").asText(null)
                        val postcode = addressNode.path("postcode").asText(null)
                        val city = addressNode.path("city").asText(null)
                            ?: addressNode.path("town").asText(null)
                            ?: addressNode.path("suburb").asText(null)

                        val bearing = calculateBearing(latitude, longitude, lat, lon)

                        places.add(
                            NearbyPlaceItem(
                                name = name,
                                category = category,
                                type = type,
                                displayName = displayName,
                                latitude = lat,
                                longitude = lon,
                                distanceMeters = round(distKm * 1000.0 * 10.0) / 10.0,
                                distanceKm = round(distKm * 100.0) / 100.0,
                                bearingFromObserver = bearing,
                                road = road,
                                houseNumber = houseNumber,
                                postcode = postcode,
                                city = city
                            )
                        )
                    }
                }
            }

            val sortedPlaces = places.sortedBy { it.distanceMeters }.take(cleanLimit)

            val response = NearbyPlacesResponse(
                observerLocation = observerDesc,
                searchCategory = cleanCategory,
                searchRadiusKm = cleanRadius,
                totalFound = sortedPlaces.size,
                places = sortedPlaces,
                note = if (sortedPlaces.isEmpty()) "No '$cleanCategory' found within $cleanRadius km." else null
            )

            nearbyPlacesCache[cacheKey] = CacheEntry(response, now)
            return response
        } catch (e: Exception) {
            logger.error("Failed to parse nearby places response: ${e.message}", e)
            return NearbyPlacesResponse(
                observerLocation = observerDesc,
                searchCategory = cleanCategory,
                searchRadiusKm = cleanRadius,
                totalFound = 0,
                places = emptyList(),
                note = "Error parsing nearby places data from OpenStreetMap."
            )
        }
    }

    fun getCurrentLocation(ip: String? = null): LocationResponse {
        val now = System.currentTimeMillis()
        val ttlMs = properties.cacheTtlMinutes * 60_000L

        if (ip.isNullOrBlank()) {
            cachedCurrentLocation?.let {
                if ((now - it.timestamp) < ttlMs) return it.data
            }
        }

        val rawJson = client.getIpGeolocation(ip)
        if (!rawJson.isNullOrBlank()) {
            try {
                val root = objectMapper.readTree(rawJson)
                if (root.path("status").asText("") == "success") {
                    val city = root.path("city").asText(properties.defaultCity)
                    val country = root.path("country").asText(properties.defaultCountry)
                    val countryCode = root.path("countryCode").asText(properties.defaultCountryCode)
                    val lat = root.path("lat").asDouble(properties.defaultLatitude)
                    val lon = root.path("lon").asDouble(properties.defaultLongitude)
                    val timezone = root.path("timezone").asText("Europe/Amsterdam")
                    val region = root.path("regionName").asText(null)
                    val zip = root.path("zip").asText(null)
                    val isp = root.path("isp").asText("")
                    val queryIp = root.path("query").asText("")

                    val desc = if (isp.isNotBlank()) "Real detected location for $city, $country ($isp, $queryIp)" else "Real detected location for $city, $country"

                    val location = LocationResponse(
                        city = city,
                        country = country,
                        countryCode = countryCode,
                        latitude = lat,
                        longitude = lon,
                        timezone = timezone,
                        description = desc,
                        region = region,
                        postalCode = zip,
                        source = "IP_GEOLOCATION"
                    )

                    if (ip.isNullOrBlank()) {
                        cachedCurrentLocation = CacheEntry(location, now)
                    }
                    return location
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse IP geolocation response: ${e.message}")
            }
        }

        // Fallback to configured default (Amsterdam headquarters)
        return LocationResponse(
            city = properties.defaultCity,
            country = properties.defaultCountry,
            countryCode = properties.defaultCountryCode,
            latitude = properties.defaultLatitude,
            longitude = properties.defaultLongitude,
            timezone = "Europe/Amsterdam",
            description = "Configured default location for ${properties.defaultCity}, ${properties.defaultCountry}",
            region = "North Holland",
            postalCode = "1011 RD",
            source = "DEFAULT_CONFIG"
        )
    }

    private fun parseAddress(node: JsonNode): OsmAddress {
        val city = node.path("city").asText(null)
            ?: node.path("town").asText(null)
            ?: node.path("village").asText(null)
            ?: node.path("municipality").asText(null)

        return OsmAddress(
            road = node.path("road").asText(null),
            houseNumber = node.path("house_number").asText(null),
            suburb = node.path("suburb").asText(null),
            neighbourhood = node.path("neighbourhood").asText(null)
                ?: node.path("quarter").asText(null),
            city = city,
            town = node.path("town").asText(null),
            municipality = node.path("municipality").asText(null),
            state = node.path("state").asText(null),
            postcode = node.path("postcode").asText(null),
            country = node.path("country").asText(null),
            countryCode = node.path("country_code").asText(null)?.uppercase()
        )
    }

    private fun parseBoundingBox(node: JsonNode): List<Double>? {
        if (!node.isArray || node.size() < 4) return null
        return listOf(
            node.get(0).asDouble(),
            node.get(1).asDouble(),
            node.get(2).asDouble(),
            node.get(3).asDouble()
        )
    }

    private fun normalizeCategory(input: String): String = when (input.lowercase()) {
        "ev", "charging", "ev_charging", "ev-charging", "charger" -> "charging_station"
        "gas", "gas_station", "petrol", "fuel" -> "fuel"
        "supermarket", "groceries", "grocery" -> "supermarket"
        "clinic", "doctor", "er", "emergency" -> "hospital"
        "pharmacy", "drugstore", "apotheek" -> "pharmacy"
        "park", "parking", "garage" -> "parking"
        "atm", "cash", "bank" -> "atm"
        "coffee", "cafe" -> "cafe"
        "lodging", "motel" -> "hotel"
        else -> input
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearingDegrees = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        val index = (round(bearingDegrees / 45.0).toInt()) % 8
        return directions[index]
    }
}
