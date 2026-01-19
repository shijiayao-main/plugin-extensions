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

    versionCatalogs {
        create("pluginLibs") {
            from(files("${rootDir}/gradle/plugin.libs.versions.toml"))
        }
    }
}

rootProject.name = "plugin-extensions"

include(":core")
include(":plugin-demo")
include(":core-extensions")
include(":plugin-trace")