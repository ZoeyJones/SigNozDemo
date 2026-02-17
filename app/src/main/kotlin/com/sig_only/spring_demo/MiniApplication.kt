package com.sig_only.spring_demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MiniApplication

fun main(args: Array<String>) {
    runApplication<MiniApplication>(*args)
}
