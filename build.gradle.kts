plugins {
    // AGP 9 has built-in Kotlin support; it also auto-applies the Compose
    // compiler when buildFeatures.compose = true, so no separate Kotlin plugins.
    alias(libs.plugins.android.application) apply false
}
