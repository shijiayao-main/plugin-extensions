plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-gradle-plugin`
    base
//    `kotlin-dsl`
}

dependencies {
    api(gradleApi())
    api(kotlin("stdlib"))
    api(pluginLibs.kotlinReflect)

    implementation(pluginLibs.googleAutoService)

    api(pluginLibs.boosterAndroidGradleApi)

    api(pluginLibs.boosterBuild)
    api(pluginLibs.apacheCommonsCompress)
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")

    api(pluginLibs.asm)
    api(pluginLibs.asmAnalysis)
    api(pluginLibs.asmCommons)
    api(pluginLibs.asmTree)
    api(pluginLibs.asmUtil)

    compileOnly(pluginLibs.androidBuildTools)

    api(project(":core-extensions"))
}

gradlePlugin {
    plugins {
        create("extensions") {
            id = "com.jiaoay.plugins"
            implementationClass = "com.jiaoay.plugins.core.ExtensionsPlugin"
        }
    }
}
