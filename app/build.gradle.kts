plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.amberplayer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.amberplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Define the current stable Media3 version
    val media3_version = "1.9.2"
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")

// Media3 - The core ExoPlayer audio engine
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-session:1.3.0") // Use whichever version matches your exoplayer
// Media3 - Enables background playback and lock-screen controls
    implementation("androidx.media3:media3-session:$media3_version")
// Media3 - Core media UI utilities
    implementation("androidx.media3:media3-ui:$media3_version")

// Palette API - Extracts dominant colors from album art for the Amberol aesthetic
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Coil for loading the album art in Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Palette API for extracting colors from the album art
    implementation("androidx.palette:palette-ktx:1.0.0")
}