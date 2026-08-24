// Top-level build file. Individual module build files apply the plugins they need.
//
// Version notes (checked against official Android/Kotlin/Gradle docs as of
// Aug 2026 - see README "Toolchain versions" section for sources and how to
// re-verify if these have moved on since):
//   - AGP 9.x ships BUILT-IN Kotlin support. The org.jetbrains.kotlin.android
//     plugin must NOT be applied alongside it - AGP compiles Kotlin itself.
//   - Jetpack Compose still needs its own compiler plugin
//     (org.jetbrains.kotlin.plugin.compose) even with built-in Kotlin,
//     because the Compose compiler is a separate Kotlin compiler plugin.
//   - KSP (used by Room) supports AGP 9 built-in Kotlin from KSP 2.3.1+.
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.4.0-2.0.4" apply false
}
