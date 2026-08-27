package com.clovercloud.jarvis.util

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtil {
    val AMSTERDAM_ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val FORMATTER_24H = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Returns current timestamp in Europe/Amsterdam timezone formatted as "yyyy-MM-dd HH:mm:ss" (24H).
     */
    fun amsterdamNowFormatted(): String {
        return ZonedDateTime.now(AMSTERDAM_ZONE).format(FORMATTER_24H)
    }

    /**
     * Returns current ZonedDateTime in Europe/Amsterdam.
     */
    fun amsterdamNow(): ZonedDateTime {
        return ZonedDateTime.now(AMSTERDAM_ZONE)
    }
}
