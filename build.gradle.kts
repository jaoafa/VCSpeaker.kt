plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.8.0"
    application
}

group = "com.jaoafa"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://maven.yuua.dev/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("junit:junit:4.13.1")

    implementation("dev.kord:kord-voice:0.9.0")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.6")
    implementation("io.github.qbosst:kordex-hybrid-commands:1.0.3-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("ch.qos.logback:logback-classic:1.3.7")
    implementation("com.sedmelluq:lavaplayer:1.3.77")
    implementation("com.uchuhimo:konf:1.1.2")
    implementation("com.github.ajalt.clikt:clikt:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.jaoafa.vcspeaker.MainKt"
    }

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