package com.sig_only.spring_demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TracingDemoController(private val tracingDemo: TracingDemo) {

    @GetMapping("/tracing-demo")
    fun demo(): TracingDemoResult {
        return tracingDemo.execute()
    }
}
