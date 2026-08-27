package com.clovercloud.jarvis.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ApplicationVersionLogger(
    @Value("\${APP_VERSION:dev}") private val appVersion: String,
    @Value("\${GIT_COMMIT:local}") private val gitCommit: String,
    @Value("\${BUILD_TIME:unknown}") private val buildTime: String
) {
    private val logger = LoggerFactory.getLogger(ApplicationVersionLogger::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        val commitShort = if (gitCommit.length > 7) gitCommit.substring(0, 7) else gitCommit
        logger.info("==================================================")
        logger.info("  JARVIS Application Started")
        logger.info("  Version    : {}", appVersion)
        logger.info("  Git Commit : {} ({})", commitShort, gitCommit)
        logger.info("  Build Time : {}", buildTime)
        logger.info("==================================================")
    }
}
