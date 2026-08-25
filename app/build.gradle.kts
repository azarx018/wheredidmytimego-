plugins {
    id("com.android.application")
    // NOTE: no org.jetbrains.kotlin.android here - AGP 9's built-in Kotlin
    // support compiles our Kotlin sources directly. Applying the old plugin
    // alongside it fails the build ("Cannot add extension with name 'kotlin'").
    id("org.jetbrains.kotlin.plugin.compose") // Compose compiler, still needed separately
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.timetrace.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.timetrace.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables.useSupportLibrary = false // not needed on API 31+, saves size
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // Split APKs per ABI to keep individual download size minimal.
    // (App Bundle / Play delivery achieves the same automatically on release.)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    // NOTE: composeOptions.kotlinCompilerExtensionVersion is gone - the
    // org.jetbrains.kotlin.plugin.compose plugin (applied above) now owns
    // the Compose compiler version, matching it to the Kotlin version
    // automatically. Per-option Compose compiler config, if ever needed,
    // goes in a top-level composeCompiler { } block instead.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Kotlin compiler options now live in this top-level block rather than
// android { kotlinOptions { ... } }, which built-in Kotlin removes.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Core / Compose (minimal set, no Material2, no extended icon pack) ---
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Using the full icon pack (not just -core) because several icons used
    // across the app (Schedule, BarChart, ChevronRight, Android) aren't
    // guaranteed to be in the small core set, and getting that wrong breaks
    // the build. R8 (isMinifyEnabled + isShrinkResources on release, see
    // below) strips every icon we don't reference from the shipped APK, so
    // this only affects debug build size, not the release artifact.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // --- Storage ---
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // --- Background work (used sparingly: daily summary notification only) ---
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
