plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "ru.killwolfvlad"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor)

    implementation(libs.bundles.kotlinx)

    compileOnly(libs.bundles.redis)
    testImplementation(libs.bundles.redis)

    testImplementation(kotlin("reflect"))

    testImplementation(libs.bundles.kotest)

    testImplementation(libs.bundles.logback)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
