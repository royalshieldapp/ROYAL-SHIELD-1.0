import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.dagger.hilt.android") version "2.48" apply false
    alias(libs.plugins.kotlin.compose)
}

// Load API keys from local.properties (never committed to VCS)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.royalshield.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.royalshield.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true

        // Secure API Keys via BuildConfig (sourced from local.properties)
        val vtKey = localProps.getProperty("VIRUSTOTAL_API_KEY", "")
        val geminiKey = localProps.getProperty("GEMINI_API_KEY", "")
        val mapsKey = localProps.getProperty("MAPS_API_KEY", "")
        val admobAppId = localProps.getProperty("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        val avKey = localProps.getProperty("ALIENVAULT_API_KEY", "")
        val twilioSid = localProps.getProperty("TWILIO_ACCOUNT_SID", "")
        val twilioToken = localProps.getProperty("TWILIO_AUTH_TOKEN", "")
        val didKey = localProps.getProperty("DID_API_KEY", "")
        val loyaltyUrl = localProps.getProperty("LOYALTY_API_URL", "https://server-beckend.onrender.com/api/loyalty")
        val apiBaseUrl = localProps.getProperty("API_BASE_URL", "https://server-beckend.onrender.com")

        buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"$vtKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "ALIENVAULT_API_KEY", "\"$avKey\"")
        buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"$twilioSid\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"$twilioToken\"")
        buildConfigField("String", "DID_API_KEY", "\"$didKey\"")
        buildConfigField("String", "LOYALTY_API_URL", "\"$loyaltyUrl\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Maps key injected into Manifest via placeholder (never hardcoded)
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_KEYSTORE_PATH", "../keystore/royal_shield.jks"))
            storePassword = localProps.getProperty("RELEASE_KEYSTORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase & Google Auth
    implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("com.google.android.gms:play-services-home:17.1.0")
    implementation("com.google.android.gms:play-services-home-types:17.1.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-ai")
    
    // Billing
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Google Play Services Location para GPS
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.1.0")
    
    // Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Moshi for JSON parsing
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
    
    // Maps Utilities for heatmap
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.maps.android:maps-utils-ktx:5.0.0")
    
    // Gson (legacy support)
    implementation("com.google.code.gson:gson:2.10.1")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // WireGuard VPN
    implementation("com.wireguard.android:tunnel:1.0.20230706")

    // EncryptedSharedPreferences (Fase 1.2)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Bouncy Castle PQC (Post-Quantum Cryptography)
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcutil-jdk18on:1.78.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // CameraX & ML Kit
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.zxing:core:3.5.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
