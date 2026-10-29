plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.biketracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.biketracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.3.0"

        val gitCommitSha = try {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
        } catch (_: Exception) {
            ""
        }
        buildConfigField("String", "GIT_SHA", "\"$gitCommitSha\"")
        buildConfigField("Long", "BUILD_TIME", "${System.currentTimeMillis()}L")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: "${System.getProperty("user.home")}/.android/bike-tracker-release.jks"
            val keystoreFile = file(keystorePath)
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: "biketracker123"
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "biketracker"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "biketracker123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(file("build/generated/assets/changelog"))
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val copyChangelog = tasks.register<Copy>("copyChangelog") {
    from("$rootDir/../frontend/CHANGELOG.md")
    into(file("build/generated/assets/changelog"))
}

tasks.named("preBuild") {
    dependsOn(copyChangelog)
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Credentials & Auth
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Location
    implementation(libs.play.services.location)

    // osmdroid Map
    implementation(libs.osmdroid.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
