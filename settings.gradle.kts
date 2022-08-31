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

rootProject.name = "plugins"

include(":core")
include(":plugin-demo")
include(":core-extensions")
enableFeaturePreview("VERSION_CATALOGS")