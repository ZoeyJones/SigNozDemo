package com.sig_only.spring_demo

data class TracingDemoResult(
    val endpointA: String,
    val endpointB: String,
    val savedAt: Long,
    val storedEntries: Int,
)
