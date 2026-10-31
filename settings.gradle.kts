pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "umstieg"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("collector")
project(":collector").projectDir = file("services/collector")
