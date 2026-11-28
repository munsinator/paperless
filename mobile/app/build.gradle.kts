plugins {
    id("com.android.application")
}

android {
    namespace = "at.technikum.paperless.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "at.technikum.paperless.mobile"
        minSdk = 29 // MediaStore.Downloads (Download ohne Speicher-Berechtigung)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val appUrl = providers.gradleProperty("appUrl").get()
        buildConfigField("String", "APP_URL", "\"$appUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
