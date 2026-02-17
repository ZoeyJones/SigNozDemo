package com.sig_only.spring_demo

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Component
class TracingDemo {

    private val logger = LoggerFactory.getLogger(TracingDemo::class.java)
    private val tracer = GlobalOpenTelemetry.getTracer("bare")
    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
            ).apply { setReadTimeout(Duration.ofSeconds(10)) }
        )
        .build()
    private val store = ConcurrentHashMap<Long, String>()

    fun execute(): TracingDemoResult {
        logger.info("Tracing demo started")

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futureA = CompletableFuture.supplyAsync({
                fetchEndpoint(
                    "fetch-endpoint-a",
                    "https://httpbin.org/get?source=endpointA",
                    1500L
                )
            }, executor)

            val futureB = CompletableFuture.supplyAsync({
                fetchEndpoint(
                    "fetch-endpoint-b",
                    "https://jsonplaceholder.typicode.com/todos/1",
                    3000L
                )
            }, executor)

            val resultA = futureA.join()
            val resultB = futureB.join()

            val savedAt = combineAndStore(resultA, resultB)

            logger.info("Tracing demo completed")

            return TracingDemoResult(
                endpointA = resultA,
                endpointB = resultB,
                savedAt = savedAt,
                storedEntries = store.size,
            )
        }
    }

    private fun fetchEndpoint(spanName: String, url: String, delayMs: Long): String {
        val span = tracer.spanBuilder(spanName)
            .setAttribute(AttributeKey.stringKey("endpoint.url"), url)
            .setAttribute(AttributeKey.longKey("delay.ms"), delayMs)
            .startSpan()

        return try {
            span.makeCurrent().use {
                logger.info("{} started — fetching {} with {}ms delay", spanName, url, delayMs)

                Thread.sleep(delayMs)

                val response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String::class.java) ?: ""

                val truncated = response.take(200)

                logger.info("{} ended — received {} chars", spanName, response.length)

                span.setAttribute(AttributeKey.longKey("response.length"), response.length.toLong())
                truncated
            }
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "unknown error")
            span.recordException(e)
            logger.error("{} failed", spanName, e)
            "error: ${e.message}"
        } finally {
            span.end()
        }
    }

    private fun combineAndStore(resultA: String, resultB: String): Long {
        val span = tracer.spanBuilder("combine-and-store")
            .startSpan()

        return try {
            span.makeCurrent().use {
                logger.info("combine-and-store started")

                Thread.sleep(1000L)

                val timestamp = System.currentTimeMillis()
                val combined = "A=${resultA.take(100)}|B=${resultB.take(100)}"
                store[timestamp] = combined

                span.setAttribute(AttributeKey.longKey("store.timestamp"), timestamp)
                span.setAttribute(AttributeKey.longKey("store.size"), store.size.toLong())

                logger.info("combine-and-store ended — saved at {}, store size={}", timestamp, store.size)

                timestamp
            }
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "unknown error")
            span.recordException(e)
            logger.error("combine-and-store failed", e)
            -1L
        } finally {
            span.end()
        }
    }
}
