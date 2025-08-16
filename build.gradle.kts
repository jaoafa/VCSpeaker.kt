import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("io.kotest.multiplatform") version "6.0.0-LOCAL"
    id("com.gradleup.shadow") version "9.0.0-rc3"
    application
}

group = "com.jaoafa"

repositories {
    mavenCentral()
    maven("https://repo.kord.dev/snapshots")
    maven("https://jitpack.io/")
    maven("https://snapshots-repo.kordex.dev")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.14.2")
    implementation(kotlin("reflect"))
    implementation("junit:junit:4.13.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.0")
    implementation("org.apache.logging.log4j:log4j-core:2.25.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.11")

    // Discord Related
    implementation("dev.kord:kord-core:0.15.0")
    implementation("dev.kord:kord-core-voice:new-voice-encryption-modes-SNAPSHOT")
    implementation("dev.kordex:kord-extensions:2.2.1-SNAPSHOT")
    implementation("dev.arbjerg:lavaplayer:2.2.3")

    // Ktor Client
    implementation("io.ktor:ktor-client-cio-jvm:3.2.3")
    implementation("io.ktor:ktor-client-cio:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-client-core:3.2.3")

    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-cio:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation("io.ktor:ktor-server-auth:3.2.3")

    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Other Libraries
    implementation("io.sentry:sentry:8.11.1")
    implementation("org.jsoup:jsoup:1.20.1")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.cloud:google-cloud-vision:3.47.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.3.3")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.uchuhimo:konf:1.1.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.jaoafa.vcspeaker.MainKt")
}

val buildVersion = project.version.toString().let {
    if (it != "unspecified") it
    else "local-build-${System.currentTimeMillis()}"
}

tasks.jar {
    manifest {
        attributes(
            "VCSpeaker-Version" to buildVersion
        )
    }
}

tasks.named("shadowJar", ShadowJar::class) {
    archiveBaseName.set("vcspeaker")
    archiveClassifier.set("all")

    println("Creating jar as version $buildVersion")

    archiveVersion.set(buildVersion)

    mergeServiceFiles()
}