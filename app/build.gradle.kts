import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // kotlin-android removed — AGP 9 compiles Kotlin via built-in Kotlin support.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Shared release keystore — NOT stored in this project. JKS + passwords live in a shared folder.
// 1Keystore/ is the shared registry (registry.json + one sub-folder per keystore); the
// sub-folder is what actually holds config.properties + the .jks, so "universe" is listed
// before its parent. Both device (/storage/emulated/0/...) and container (/root/storage/...)
// layouts are tried.
// Override via env: AB_KEYSTORE_DIR=/path/to/keystore
// If no folder is reachable the release build still compiles (unsigned) so R8/shrink is never blocked.
val sharedKeystoreDir = run {
    val envDir = System.getenv("AB_KEYSTORE_DIR")
    if (!envDir.isNullOrBlank() && File("$envDir/config.properties").exists()) return@run envDir
    if (!envDir.isNullOrBlank() && File(envDir).exists()) return@run envDir
    val candidates = listOf(
        "/root/storage/PhoneIDEProjects/1Keystore/universe",
        "/storage/emulated/0/PhoneIDEProjects/1Keystore/universe",
        "/root/storage/PhoneIDEProjects/1Keystore",
        "/storage/emulated/0/AndroidCSProjects/keystore",
        "/storage/emulated/0/AndroidCSProjects/1keystore",
        "/root/storage/AndroStudioProjects/1keystore",
        "/root/storage/AndroStudioProjects/keystore"
    )
    candidates.firstOrNull { File("$it/config.properties").exists() }
        ?: candidates.firstOrNull { File(it).exists() }
        ?: envDir?.takeIf { it.isNotBlank() }
        ?: "/root/storage/PhoneIDEProjects/1Keystore/universe"
}
val signingProps = Properties().apply {
    File("$sharedKeystoreDir/config.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val hasReleaseSigning =
    File("$sharedKeystoreDir/${signingProps.getProperty("storeFile", "Universe-release.jks")}").exists()

android {
    namespace = "com.io.git.way"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.io.git.way"
        minSdk = 26
        targetSdk = 36
        versionCode = 217
        versionName = "2.6.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(sharedKeystoreDir, signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    packaging {
        resources {
            // Duplicate license/notice files pulled in transitively by okhttp/kotlin
            // metadata jars — safe to drop, they don't affect app behavior and are a
            // common source of "2+ files with the same path" packaging bloat/failures.
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
            freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.kotlinx.serialization)
    implementation(libs.squareup.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    // Only used for request/response logging during development — never linked
    // into the release build (also keeps its transitive size out of release DEX).
    debugImplementation(libs.squareup.okhttp.logging.interceptor)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
