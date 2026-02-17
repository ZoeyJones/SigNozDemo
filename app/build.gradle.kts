plugins {
    alias(libs.plugins.spring.boot)
}

springBoot {
    buildInfo()
}

val otelAgent by configurations.creating { isTransitive = false }

val copyOtelAgent by
    tasks.registering(Copy::class) {
        from(otelAgent)
        into(
            tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").flatMap { it.destinationDirectory }
        )
        rename { "opentelemetry-javaagent.jar" }
    }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { finalizedBy(copyOtelAgent) }

dependencies {
    otelAgent(libs.opentelemetry.javaagent)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.opentelemetry.api)
}
