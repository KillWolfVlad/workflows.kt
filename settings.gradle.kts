plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "workflows"

include("exampleApp")
include("loadTest")
