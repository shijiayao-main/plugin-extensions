plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    compileOnly(project(":core"))

    compileOnly(pluginLibs.androidBuildTools)
}
