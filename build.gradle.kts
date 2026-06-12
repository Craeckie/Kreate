// Override the R8 version bundled with AGP 8.13.2 (supports Kotlin metadata ≤ 2.2).
// Kotlin 2.4.0 emits metadata version 2.4.0, which the bundled R8 cannot parse — it floods
// the release build with "incompatible version of Kotlin / expected 2.2.0" errors and
// degrades Kotlin-aware shrinking. R8 9.1.29 is the first release that understands Kotlin 2.4
// metadata (see https://developer.android.com/studio/build/kotlin-d8-r8-versions). Forcing it
// on the buildscript classpath makes AGP use it instead of its bundled copy.
buildscript {
    repositories {
        maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:9.1.29")
    }
}

plugins {
    // Multiplatform
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    // Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false

    // Others
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias( libs.plugins.kotlin.jvm ) apply false
}
