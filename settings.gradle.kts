pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

//    versionCatalogs {
//        create("libs") {
//            from(files("./gradle/libs.versions.toml"))
//        }
//    }
}

rootProject.name = "plugin-extensions"

include(":core")
include(":plugin-demo")
include(":core-extensions")
include(":plugin-trace")