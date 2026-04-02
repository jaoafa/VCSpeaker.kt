import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest)
    alias(libs.plugins.shadow)
    alias(libs.plugins.application)
}

group = "com.jaoafa"

repositories {
    mavenCentral()
    maven("https://snapshots.kord.dev")
    maven("https://jitpack.io")
    maven("https://snapshots-repo.kordex.dev")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.tomacheese.com")
    maven("https://maven.arbjerg.dev/snapshots")
}

dependencies {
    testImplementation(libs.bundles.testing)

    implementation(libs.bundles.logging)
    implementation(libs.bundles.discord)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.database)
    implementation(libs.bundles.other)
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

tasks.register<JavaExec>("generateMigration") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.jaoafa.vcspeaker.database.GenerateMigrationKt"

    doFirst {
        project.properties["databaseUrl"]?.let { environment["DATABASE_URL"] = it }
        project.properties["migrationName"]?.let { environment["MIGRATION_NAME"] = it }
    }
}

tasks.register<JavaExec>("runMigration") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.jaoafa.vcspeaker.database.RunMigrationKt"

    doFirst {
        project.properties["databaseUrl"]?.let { environment["DATABASE_URL"] = it }
    }
}
