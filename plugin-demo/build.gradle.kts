plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-gradle-plugin`
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(project(":core"))

    compileOnly(libs.androidBuildTools)
}