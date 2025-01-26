import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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

    testImplementation(libs.bundles.mockk)

    testImplementation(libs.bundles.logback)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<KotlinCompilationTask<*>>("compileTestKotlin").configure {
    compilerOptions.freeCompilerArgs.add("-opt-in=ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance")
    compilerOptions.freeCompilerArgs.add("-opt-in=io.lettuce.core.ExperimentalLettuceCoroutinesApi")
}

kotlin {
    jvmToolchain(21)
}
