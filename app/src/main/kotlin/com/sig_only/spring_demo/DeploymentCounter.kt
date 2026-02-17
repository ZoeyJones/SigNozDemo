package com.sig_only.spring_demo

import io.opentelemetry.api.GlobalOpenTelemetry
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

@Component
class DeploymentCounter : SmartLifecycle {

    private val meter = GlobalOpenTelemetry.getMeter("bare")
    private val counter = meter.counterBuilder("app.deployments").build()
    private var running = false

    override fun start() {
        counter.add(1)
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }
}
