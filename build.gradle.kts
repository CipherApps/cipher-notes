import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion

plugins {
    id("com.android.application") version "8.7.3" apply true
    id("org.jetbrains.kotlin.android") version "2.0.21" apply true
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply true
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply true
    id("com.google.dagger.hilt.android") version "2.51.1" apply true
}

configure<ApplicationExtension> {
    namespace = "dev.cipher.notes"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.cipher.notes"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        includeInApk = false
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
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    add("implementation", "androidx.core:core-ktx:1.15.0")
    add("implementation", "androidx.appcompat:appcompat:1.7.0")
    add("implementation", "com.google.android.material:material:1.12.0")
    add("implementation", "androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    add("implementation", "androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    add("implementation", composeBom)
    add("implementation", "androidx.compose.ui:ui")
    add("implementation", "androidx.compose.ui:ui-graphics")
    add("implementation", "androidx.compose.ui:ui-tooling-preview")
    add("implementation", "androidx.compose.material3:material3")
    add("implementation", "androidx.compose.material:material-icons-extended")

    add("implementation", "androidx.navigation:navigation-compose:2.8.4")
    add("implementation", "androidx.hilt:hilt-navigation-compose:1.2.0")

    val roomVersion = "2.6.1"
    add("implementation", "androidx.room:room-runtime:$roomVersion")
    add("implementation", "androidx.room:room-ktx:$roomVersion")
    add("ksp", "androidx.room:room-compiler:$roomVersion")

    add("implementation", "com.google.dagger:hilt-android:2.51.1")
    add("ksp", "com.google.dagger:hilt-compiler:2.51.1")

    add("implementation", "androidx.security:security-crypto:1.1.0-alpha06")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    add("implementation", "androidx.datastore:datastore-preferences:1.1.1")
    add("implementation", "androidx.core:core-splashscreen:1.0.1")

    add("debugImplementation", "androidx.compose.ui:ui-tooling")
    add("implementation", "androidx.biometric:biometric-ktx:1.2.0-alpha05")
}
