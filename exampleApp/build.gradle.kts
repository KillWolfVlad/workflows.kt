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

    implementation(kotlin("reflect"))

    implementation(libs.bundles.logback)
}

kotlin {
    jvmToolchain(21)
}
