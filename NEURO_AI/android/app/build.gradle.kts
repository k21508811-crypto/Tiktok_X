plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.neuro.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "ai.neuro.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            System.getenv("RELEASE_STORE_FILE")?.let { storeFile = file(it) }
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
}
