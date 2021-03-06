buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt").version(Versions.DETEKT_RUNTIME)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config = files("${project.rootDir}/detekt/config.yml")
    input = files(projectDir)
}
