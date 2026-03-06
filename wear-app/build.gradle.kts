plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
}

android {
    namespace = "com.heartrate.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.heartrate.wear"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildToolsVersion = "36.0.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(project(":shared"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Koin
    implementation(libs.koin.android)
}
