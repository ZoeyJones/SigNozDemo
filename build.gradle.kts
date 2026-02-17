plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
}

apply(from = "gradle/kotlin-conventions.gradle")

allprojects {
    group = "com.demo.sig_only"
    version = "0.0.1-SNAPSHOT"
}
