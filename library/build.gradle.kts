import Versions.VERSION_NAME

plugins {
    id("com.android.library")
    id("maven-publish")
    kotlin("android")
}

group = "com.github.vinted"

android {
    compileSdk = Versions.COMPILE_SDK_VERSION

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        minSdk = Versions.MIN_SDK_VERSION
    }

    buildTypes {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xopt-in=kotlin.contracts.ExperimentalContracts")
        }
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.vinted.coper"
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
    implementation(Libs.VIEW_BINDING_DELEGATE)
    testImplementation(Libs.MOCKITO_KOTLIN)
    testImplementation(Libs.KOTLIN_TESTS)
    testImplementation(Libs.JUNIT)
    testImplementation(Libs.MOCKITO)
    testImplementation(Libs.ROBOLECTRIC)
    testImplementation(Libs.MOCKITO_INLINE)
    testImplementation(Libs.KOTLIN_COROUTINES_TEST)
}
