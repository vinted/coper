object Versions {
    const val APP_COMPAT = "1.1.0"
    const val KOTLIN = "1.3.71"
    const val COROUTINES = "1.3.5"
    const val ANDROID_GRADLE_PLUGIN = "3.6.2"
    const val JUNIT = "4.13"
    const val ROBOLECTRIC = "4.3"
    const val MOCKITO = "3.1.0"
    const val MOCKITO_KOTILN = "1.5.0"
    const val CORE_KTX = "1.2.0"
    const val FRAGMENT_KTX = "1.2.4"

    const val COMPILE_SDK_VERSION = 29
    const val MIN_SDK_VERSION = 21
    const val TARGET_SDK_VERSION = 29
    const val BUILD_TOOLS_VERSION = "29.0.2"

    const val VERSION = 1
    const val VERSION_CODE = "1.0"
}

object Libs {
    const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
    const val APP_COMPAT = "androidx.appcompat:appcompat:${Versions.APP_COMPAT}"
    const val KOTLIN_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}"
    const val JUNIT = "junit:junit:${Versions.JUNIT}"
    const val ROBOLECTRIC = "org.robolectric:robolectric:${Versions.ROBOLECTRIC}"
    const val MOCKITO = "org.mockito:mockito-core:${Versions.MOCKITO}"
    const val MOCKITO_KOTLIN = "com.nhaarman:mockito-kotlin:${Versions.MOCKITO_KOTILN}"
    const val CORE_KTX = "androidx.core:core-ktx:${Versions.CORE_KTX}"
    const val KOTLIN_TESTS = "org.jetbrains.kotlin:kotlin-test:${Versions.KOTLIN}"
    const val FRAGMENT_KTX = "androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}"
}
