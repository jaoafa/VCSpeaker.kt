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
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation("junit:junit:4.13.1")

    implementation("ch.qos.logback:logback-classic:1.3.7")
    implementation("com.github.aikaterna:lavaplayer-natives:original-SNAPSHOT")
    implementation("com.github.ajalt.clikt:clikt:4.0.0")
    implementation("com.github.ajalt.clikt:clikt:4.0.0")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.6.0-SNAPSHOT")
    implementation("com.uchuhimo:konf:1.1.2")
    implementation("dev.arbjerg:lavaplayer:2.0.2")
    implementation("dev.kord:kord-core:0.12.0")
    implementation("dev.kord:kord-core-voice:0.12.0")
    implementation("dev.kord:kord-voice:0.12.0")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.sentry:sentry:6.30.0")
    implementation("net.htmlparser.jericho:jericho-html:3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.reflections:reflections:0.10.2")
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