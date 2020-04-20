plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(Versions.COMPILE_SDK_VERSION)
    buildToolsVersion(Versions.BUILD_TOOLS_VERSION)

    defaultConfig {
        minSdkVersion(Versions.MIN_SDK_VERSION)
        targetSdkVersion(Versions.TARGET_SDK_VERSION)
        versionCode = Versions.VERSION
        versionName = Versions.VERSION_CODE
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }

        kotlinOptions {
            freeCompilerArgs = listOf("-Xopt-in=kotlin.contracts.ExperimentalContracts")

        }
    }
}

tasks {
    val main = android.sourceSets["main"]
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(main.java.srcDirs)
    }

    val javaDoc by creating(Javadoc::class) {
        isFailOnError = false
        source = main.java.sourceFiles
        classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
    }

    val javaDocJar by creating(Jar::class) {
        dependsOn.add(javaDoc)
        archiveClassifier.set("javadoc")
        from(javaDoc.destinationDir)
    }

    artifacts {
        archives(sourcesJar)
        archives(javaDocJar)
    }
}

dependencies {
    api(Libs.APP_COMPAT)
    api(Libs.KOTLIN_COROUTINES)
    testImplementation(Libs.MOCKITO_KOTLIN)
    testImplementation(Libs.KOTLIN_TESTS)
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.MOCKITO)
    testImplementation(Libs.ROBOLECTRIC)
    testImplementation(Libs.MOCKITO_INLINE)
}
