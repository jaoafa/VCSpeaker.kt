plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "2.0.0"
    application
}

group = "com.jaoafa"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://maven.yuua.dev/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.11")
    implementation(kotlin("reflect"))
    implementation("junit:junit:4.13.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")

    // Discord Related
    implementation("dev.kord:kord-core:unknown-d-field-fix-SNAPSHOT")
    implementation("dev.kord:kord-core-voice:0.14.0")
    implementation("dev.kord:kord-voice:0.13.1")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.6.0")
    implementation("dev.arbjerg:lavaplayer:2.2.0")
    implementation("com.github.aikaterna:lavaplayer-natives:original-SNAPSHOT")

    // Ktor
    implementation("io.ktor:ktor-client-cio-jvm:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-client-core:2.3.11")

    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Other Libraries
    implementation("io.sentry:sentry:7.11.0")
    implementation("net.htmlparser.jericho:jericho-html:3.4")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.cloud:google-cloud-vision:3.32.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.3")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("com.uchuhimo:konf:1.1.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.jaoafa.vcspeaker.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.jaoafa.vcspeaker.MainKt"
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveBaseName.set("vcspeaker-kt")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith("jar")
        }.map { zipTree(it) }
    })
}