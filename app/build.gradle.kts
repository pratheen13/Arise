plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.arise.myapp1"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.arise.myapp1"
        minSdk = 28
        targetSdk = 36
        versionCode = 8
        versionName = "4.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Release signing is optional and supplied through environment variables, never committed.
    val releaseKey = System.getenv("ARISE_KEYSTORE")
    signingConfigs {
        if (releaseKey != null) create("arise") {
            storeFile = file(releaseKey)
            storePassword = System.getenv("ARISE_STORE_PASSWORD")
            keyAlias = System.getenv("ARISE_KEY_ALIAS") ?: "arise"
            keyPassword = System.getenv("ARISE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKey != null) signingConfig = signingConfigs.getByName("arise")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.health.connect:connect-client:1.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

