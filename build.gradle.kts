plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.cipher.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.cipher.notes"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.4.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src/main/kotlin")
            res.srcDirs("res")
            assets.srcDirs("assets")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    implementation(libs.security.crypto)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.prefs)
    implementation(libs.splashscreen)

    debugImplementation(libs.compose.ui.tooling)
}
