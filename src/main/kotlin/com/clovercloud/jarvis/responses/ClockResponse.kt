package com.clovercloud.jarvis.responses

data class ClockResponse(
    val isoTimestamp: String,
    val formattedTime: String,
    val formattedDate: String,
    val dayOfWeek: String,
    val timeZone: String,
    val epochSeconds: Long
)
