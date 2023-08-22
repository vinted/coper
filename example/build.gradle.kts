plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = Versions.COMPILE_SDK_VERSION

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        applicationId = "com.vinted.coper.example"
        minSdk = Versions.MIN_SDK_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.vinted.coper.example"
}

dependencies {
    implementation(project(":library"))
    implementation(Libs.KOTLIN_STDLIB)
    implementation(Libs.APP_COMPAT)
    implementation(Libs.CORE_KTX)
    implementation(Libs.KOTLIN_COROUTINES)
    implementation(Libs.FRAGMENT_KTX)
    implementation(Libs.VIEW_BINDING_DELEGATE)
    testImplementation(Libs.JUNIT)
}
