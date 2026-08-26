package com.clovercloud.jarvis.controllers

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/development")
@Tag(name = "Development")
class DevelopmentController {

    @ApiResponse(responseCode = "200", description = "Returns a simple health check response")
    @GetMapping("/health")
    fun health(): ResponseEntity<String> = ResponseEntity("Healthy", HttpStatus.OK)

    @ApiResponse(responseCode = "200", description = "Returns a simple pong response")
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> = ResponseEntity("pong", HttpStatus.OK)

}
