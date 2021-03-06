import Versions.VERSION_NAME

plugins {
    id("com.android.library")
    id("maven-publish")
    kotlin("android")
    kotlin("android.extensions")
}

group = "com.github.vinted"

android {
    compileSdkVersion(Versions.COMPILE_SDK_VERSION)

    defaultConfig {
        minSdkVersion(Versions.MIN_SDK_VERSION)
    }

    buildTypes {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xopt-in=kotlin.contracts.ExperimentalContracts")
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "$group"
                artifactId = "coper"
                version = VERSION_NAME
                from(components["release"])
            }
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
        source = main.java.getSourceFiles()
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
    implementation(Libs.LIFECYCLE)
    implementation(Libs.FRAGMENT_KTX)
    testImplementation(Libs.MOCKITO_KOTLIN)
    testImplementation(Libs.KOTLIN_TESTS)
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.MOCKITO)
    testImplementation(Libs.ROBOLECTRIC)
    testImplementation(Libs.MOCKITO_INLINE)
}
