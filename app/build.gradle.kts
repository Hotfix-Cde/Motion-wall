plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.motionwall"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.motionwall"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "2.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing is wired to the GitHub Actions workflow. When the
            // workflow restores the keystore from the secrets and sets
            // the environment variables below, the release APK is signed
            // automatically. Locally, without those variables, an
            // unsigned APK is produced.
            signingConfig = runCatching {
                val storePath = System.getenv("MOTIONWALL_KEYSTORE_PATH")
                val storePass = System.getenv("MOTIONWALL_KEYSTORE_PASSWORD")
                val keyAliasEnv = System.getenv("MOTIONWALL_KEY_ALIAS")
                val keyPass = System.getenv("MOTIONWALL_KEY_PASSWORD")
                if (!storePath.isNullOrBlank() && !storePass.isNullOrBlank()
                    && !keyAliasEnv.isNullOrBlank() && !keyPass.isNullOrBlank()
                ) {
                    signingConfigs.create("ci") {
                        storeFile = file(storePath)
                        storePassword = storePass
                        keyAlias = keyAliasEnv
                        keyPassword = keyPass
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")

    // Media3 ExoPlayer for the in-app video preview
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
