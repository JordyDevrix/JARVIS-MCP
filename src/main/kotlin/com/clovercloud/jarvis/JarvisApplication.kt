package com.clovercloud.jarvis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JarvisApplication

fun main(args: Array<String>) {
    runApplication<JarvisApplication>(*args)
}
