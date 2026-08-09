plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hotfixcde.motionwall"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hotfixcde.motionwall"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "1.1.1"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("MOTIONWALL_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MOTIONWALL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MOTIONWALL_KEY_ALIAS")
                keyPassword = System.getenv("MOTIONWALL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.android.material:material:1.12.0")

    val media3Version = "1.6.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
}
