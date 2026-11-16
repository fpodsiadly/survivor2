import java.text.SimpleDateFormat
import java.util.Date
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.surv20.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.surv20.game"
        minSdk = 26
        targetSdk = 36
        // Bump on every commit that changes the app (PLAN.md K9).
        versionCode = 1
        versionName = "0.1.0"
    }

    // Fixed signing key from CI secrets so every build installs as an update; locally the debug key is used.
    signingConfigs {
        create("shared") {
            val path = System.getenv("KEYSTORE_PATH")
            if (path != null) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
        release {
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Build time shown next to the version on the main screen.
    val buildStamp = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
    buildTypes.all {
        buildConfigField("String", "BUILD_TIME", "\"$buildStamp\"")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The game (rules, screens, images) lives in :shared, common with iOS.
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.10.1")
}
