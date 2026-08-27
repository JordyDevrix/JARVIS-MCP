package com.clovercloud.jarvis.controllers

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/development")
@Tag(name = "Development")
class DevelopmentController(
    @Value("\${APP_VERSION:dev}") private val appVersion: String = "dev",
    @Value("\${GIT_COMMIT:local}") private val gitCommit: String = "local",
    @Value("\${BUILD_TIME:unknown}") private val buildTime: String = "unknown"
) {

    @ApiResponse(responseCode = "200", description = "Returns a simple health check response")
    @GetMapping("/health")
    fun health(): ResponseEntity<String> = ResponseEntity("Healthy", HttpStatus.OK)

    @ApiResponse(responseCode = "200", description = "Returns application version and deployment metadata")
    @GetMapping("/version")
    fun version(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(
        mapOf(
            "version" to appVersion,
            "gitCommit" to gitCommit,
            "buildTime" to buildTime
        )
    )
}