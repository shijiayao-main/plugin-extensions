buildscript {
    dependencies {
        classpath(libs.kotlinGradlePlugin)
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    }
}

plugins {
    alias(libs.plugins.kotlin) apply false
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}