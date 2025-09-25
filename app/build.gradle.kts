plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "fi.kidozz.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "fi.kidozz.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // debug setup
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("local") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://kiddozz-production.up.railway.app\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://prod-api.kiddozz.com\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // --- Compose Core ---
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")

    // --- Accompanist (animations, insets, flow layouts) ---
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // --- Material Icons ---
    implementation("androidx.compose.material:material-icons-core:1.6.8")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // --- Lottie for animations ---
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    
    // --- Google Fonts ---
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")

    // --- Existing project dependencies (leave untouched) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}