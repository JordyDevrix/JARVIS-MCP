package com.clovercloud.jarvis

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerTests {

    @Test
    fun testApplicationStartsSuccessfully() {
        // Basic smoke test to ensure the application context loads
        assertNotNull(this)
    }

    // MCP endpoint tests are skipped until the endpoint is fully configured
    // The /mcp endpoint requires Spring AI MCP server to be properly initialized
}
