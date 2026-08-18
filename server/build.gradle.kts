plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("dev.hawk0f.chess.server.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.ktor.client.content.negotiation)
}
