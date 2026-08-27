package com.clovercloud.jarvis

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerTests {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun testMcpInitializeAndListTools() {
        val client = RestClient.create("http://localhost:$port")

        // 1. Initialize MCP
        val initRequest = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {
                        "name": "test-client",
                        "version": "1.0.0"
                    }
                }
            }
        """.trimIndent()

        val initResponseEntity = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(initRequest)
            .retrieve()
            .toEntity(String::class.java)

        println("MCP Init Response Headers: ${initResponseEntity.headers}")
        val sessionId = initResponseEntity.headers.getFirst("mcp-session-id")
        println("Session ID: $sessionId")

        val initResponse = initResponseEntity.body
        println("MCP Init Response Body: $initResponse")
        assertNotNull(initResponse)
        assertTrue(initResponse!!.contains("capabilities"))
        assertTrue(initResponse.contains("JARVIS"))

        // 2. notifications/initialized
        val notifyRequest = """
            {
                "jsonrpc": "2.0",
                "method": "notifications/initialized"
            }
        """.trimIndent()

        val notifySpec = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(notifyRequest)
        if (sessionId != null) {
            notifySpec.header("mcp-session-id", sessionId)
        }
        notifySpec.retrieve().toBodilessEntity()

        // 3. tools/list
        val listRequest = """
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/list",
                "params": {}
            }
        """.trimIndent()

        val listSpec = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(listRequest)
        if (sessionId != null) {
            listSpec.header("mcp-session-id", sessionId)
        }
        val listResponse = listSpec.retrieve().body(String::class.java)
        println("MCP Tools List: $listResponse")
        assertNotNull(listResponse)
        assertTrue(listResponse!!.contains("get_current_time"))
        assertTrue(listResponse.contains("get_current_location"))
        assertTrue(listResponse.contains("lookup_location"))

        // 4. tools/call get_current_time
        val callTimeRequest = """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "get_current_time",
                    "arguments": {
                        "timezone": "Europe/Amsterdam"
                    }
                }
            }
        """.trimIndent()

        val callTimeSpec = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(callTimeRequest)
        if (sessionId != null) {
            callTimeSpec.header("mcp-session-id", sessionId)
        }
        val callTimeResponse = callTimeSpec.retrieve().body(String::class.java)
        println("MCP Call Time Response: $callTimeResponse")
        assertNotNull(callTimeResponse)
        assertTrue(callTimeResponse!!.contains("Europe/Amsterdam"))

        // 5. tools/call get_current_location
        val callLocRequest = """
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "get_current_location",
                    "arguments": {}
                }
            }
        """.trimIndent()

        val callLocSpec = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(callLocRequest)
        if (sessionId != null) {
            callLocSpec.header("mcp-session-id", sessionId)
        }
        val callLocResponse = callLocSpec.retrieve().body(String::class.java)
        println("MCP Call Current Location Response: $callLocResponse")
        assertNotNull(callLocResponse)
        assertTrue(callLocResponse!!.contains("Amsterdam"))
        assertTrue(callLocResponse.contains("Netherlands"))

        // 6. tools/call lookup_location
        val callLookupRequest = """
            {
                "jsonrpc": "2.0",
                "id": 5,
                "method": "tools/call",
                "params": {
                    "name": "lookup_location",
                    "arguments": {
                        "city": "Tokyo"
                    }
                }
            }
        """.trimIndent()

        val callLookupSpec = client.post()
            .uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body(callLookupRequest)
        if (sessionId != null) {
            callLookupSpec.header("mcp-session-id", sessionId)
        }
        val callLookupResponse = callLookupSpec.retrieve().body(String::class.java)
        println("MCP Call Lookup Location Response: $callLookupResponse")
        assertNotNull(callLookupResponse)
        assertTrue(callLookupResponse!!.contains("Tokyo"))
        assertTrue(callLookupResponse.contains("Japan"))
    }
}
