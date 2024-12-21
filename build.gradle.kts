import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.kotest.multiplatform") version "5.9.1"
    id("com.gradleup.shadow") version "8.3.5"
    application
}

group = "com.jaoafa"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.kordex.dev/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.14")
    implementation(kotlin("reflect"))
    implementation("junit:junit:4.13.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Discord Related
    implementation("dev.kord:kord-core:0.15.0")
    implementation("dev.kord:kord-core-voice:0.15.0")
    implementation("dev.kordex:kord-extensions:2.2.1-SNAPSHOT")
    implementation("dev.arbjerg:lavaplayer:2.2.2")

    // Ktor
    implementation("io.ktor:ktor-client-cio-jvm:3.0.1")
    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-client-core:3.0.1")

    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Other Libraries
    implementation("io.sentry:sentry:7.18.0")
    implementation("org.jsoup:jsoup:1.18.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.cloud:google-cloud-vision:3.47.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.3.0")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.uchuhimo:konf:1.1.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.jaoafa.vcspeaker.MainKt")
}

tasks.named("shadowJar", ShadowJar::class) {
    archiveBaseName.set("vcspeaker-kt")
    archiveClassifier.set("all")
    archiveVersion.set("")
}
