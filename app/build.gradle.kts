plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.persistence"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.persistence"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- Firebase and Authentication Dependencies ---

    // 1. Use the Firebase BoM to manage Firebase versions
    // This ensures that all Firebase libraries are compatible with each other.
    implementation(platform("com.google.firebase:firebase-bom:28.4.1"))

    // 2. Declare Firebase dependencies *without* versions when using the BoM
    // The BoM (declared above) provides the correct versions for these.
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // 3. FirebaseUI Authentication and its required Google Play Services dependencies
    // FirebaseUI Auth itself is NOT part of the Firebase BoM.
    // We need to specify its version explicitly, and it needs the Play Services Auth dependency.

    // Required by FirebaseUI Auth for features like Google Sign-In (Credential Manager)
    // This dependency needs an explicit version and is NOT managed by the Firebase BoM.
    implementation("com.google.android.gms:play-services-auth:20.7.0") // Keep this version or update to a newer one if needed

    // Required by FirebaseUI Auth for Phone Authentication with Smart Lock auto-verification
    // This dependency needs an explicit version and is NOT managed by the Firebase BoM.
    implementation("com.google.android.gms:play-services-auth-api-phone:18.2.0") // Keep this version

    // Credential Manager libraries (Keep these if you are intentionally using them)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.navigation:navigation-compose:2.5.3")

    // Multidex support library
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-splashscreen:1.0.1")


    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    implementation("androidx.compose.material:material-icons-extended")
}
