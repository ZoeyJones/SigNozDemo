package com.sig_only.spring_demo

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class MemoryGauge : SmartLifecycle {

    private val logger = LoggerFactory.getLogger(MemoryGauge::class.java)
    private val meter = GlobalOpenTelemetry.getMeter("bare")
    private val pollCounter = AtomicInteger(0)
    private val memoryValue = AtomicLong(0)
    private var scheduler: ScheduledExecutorService? = null
    private var running = false

    override fun start() {
        meter.gaugeBuilder("app.memory.used")
            .setUnit("bytes")
            .ofLongs()
            .buildWithCallback { measurement ->
                val count = pollCounter.get()
                val attributes = Attributes.of(POLL_COUNT_KEY, count.toLong())
                measurement.record(memoryValue.get(), attributes)
            }

        scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "memory-gauge").apply { isDaemon = true }
        }
        scheduler!!.scheduleAtFixedRate(::recordMemory, 0, 5, TimeUnit.SECONDS)
        running = true
    }

    override fun stop() {
        scheduler?.shutdown()
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    private fun recordMemory() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val count = pollCounter.incrementAndGet()

        memoryValue.set(usedMemory)

        logger.info("Memory poll #{}: used={}MB", count, usedMemory / 1_048_576)
    }

    companion object {
        private val POLL_COUNT_KEY = AttributeKey.longKey("poll.count")
    }
}
