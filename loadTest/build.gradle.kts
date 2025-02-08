plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))

    implementation(libs.bundles.redis)

    implementation(libs.bundles.jedis)

    implementation(libs.bundles.logback)
}

tasks.register<JavaExec>("Redis Standalone - 100K - Lettuce") {
    group = "load test"

    mainClass.set("ru.killwolfvlad.workflows.loadTest.LoadTestKt")
    classpath = sourceSets.main.get().runtimeClasspath

    jvmArgs =
        listOf(
            "-Dsize=100000",
            "-DKeyValueClient=LettuceRedisClient",
            "-XX:StartFlightRecording:filename=Redis Standalone - 100K - Lettuce.jfr",
        )
}

tasks.register<JavaExec>("Redis Standalone - 100K - ReThis") {
    group = "load test"

    mainClass.set("ru.killwolfvlad.workflows.loadTest.LoadTestKt")
    classpath = sourceSets.main.get().runtimeClasspath

    jvmArgs =
        listOf(
            "-Dsize=100000",
            "-DKeyValueClient=ReThisRedisClient",
            "-XX:StartFlightRecording:filename=Redis Standalone - 100K - ReThis.jfr",
        )
}

kotlin {
    jvmToolchain(21)
}
