object Versions {
    const val APP_COMPAT = "1.1.0"
    const val KOTLIN = "1.5.10"
    const val COROUTINES = "1.5.0"
    const val ANDROID_GRADLE_PLUGIN = "4.2.1"
    const val JUNIT = "4.13"
    const val ROBOLECTRIC = "4.3"
    const val MOCKITO = "3.11.2"
    const val MOCKITO_KOTILN = "3.2.0"
    const val CORE_KTX = "1.2.0"
    const val FRAGMENT_KTX = "1.2.4"
    const val DETEKT_RUNTIME = "1.17.1"
    const val LIFECYCLE = "2.2.0"

    const val COMPILE_SDK_VERSION = 29
    const val MIN_SDK_VERSION = 21

    private const val MAJOR = 0
    private const val MINOR = 4
    private const val PATCH = 0

    const val VERSION_NAME: String = "$MAJOR.$MINOR.$PATCH"
}

object Libs {
    const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
    const val APP_COMPAT = "androidx.appcompat:appcompat:${Versions.APP_COMPAT}"
    const val KOTLIN_COROUTINES =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}"
    const val JUNIT = "junit:junit:${Versions.JUNIT}"
    const val ROBOLECTRIC = "org.robolectric:robolectric:${Versions.ROBOLECTRIC}"
    const val MOCKITO = "org.mockito:mockito-core:${Versions.MOCKITO}"
    const val MOCKITO_KOTLIN = "org.mockito.kotlin:mockito-kotlin:${Versions.MOCKITO_KOTILN}"
    const val CORE_KTX = "androidx.core:core-ktx:${Versions.CORE_KTX}"
    const val KOTLIN_TESTS = "org.jetbrains.kotlin:kotlin-test:${Versions.KOTLIN}"
    const val FRAGMENT_KTX = "androidx.fragment:fragment-ktx:${Versions.FRAGMENT_KTX}"
    const val MOCKITO_INLINE = "org.mockito:mockito-inline:${Versions.MOCKITO}"
    const val LIFECYCLE = "androidx.lifecycle:lifecycle-common-java8:${Versions.LIFECYCLE}"
}
