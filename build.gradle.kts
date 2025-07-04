import com.diffplug.gradle.spotless.SpotlessExtension

buildscript {
    dependencies {
        classpath(libs.kotlinGradlePlugin)
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    }
}

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.spotless).apply(false)
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}


subprojects {
    project.afterEvaluate {
        apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

        if (project.file("build.gradle").exists().not() && project.file("build.gradle.kts").exists()
                .not()
        ) {
            return@afterEvaluate
        }

        configure<SpotlessExtension>() {
            kotlin {
                target("**/*.kt")
                ktlint("1.6.0").editorConfigOverride(
                    mapOf(
                        "android" to "true",
                    ),
                )
            }
            java {
                target("**/*.java")
                googleJavaFormat()
                leadingTabsToSpaces(2)
                trimTrailingWhitespace()
                removeUnusedImports()
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint("1.6.0")
            }
        }
    }
}