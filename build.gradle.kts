import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    `maven-publish`

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
}

group = "ru.killwolfvlad"
version = providers.gradleProperty("version").getOrElse("1.0-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.kotlinx)

    api(libs.bundles.redis)
    testImplementation(libs.bundles.redis)

    testImplementation(kotlin("reflect"))

    testImplementation(libs.bundles.kotest)

    testImplementation(libs.bundles.jedis)

    testImplementation(libs.bundles.mockk)

    testImplementation(libs.bundles.logback)
}

tasks.check {
    dependsOn("installKotlinterPrePushHook")
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/KillWolfVlad/workflows.kt")

            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }

    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.from(files("$rootDir/detekt.yaml"))
}
