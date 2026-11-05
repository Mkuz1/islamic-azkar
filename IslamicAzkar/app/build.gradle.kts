import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "com.koussa.islamicazkar"
    compileSdk = 34
    compileSdkExtension = 12
    buildToolsVersion = "35.0.1"

    defaultConfig {
        applicationId = "com.koussa.islamicazkar"
        minSdk = 26
        targetSdk = 36
        // The GitHub release tag must be "v" + versionName (e.g. v1.0.5): the github build compares them.
        versionCode = 11
        versionName = "1.0.11"
        vectorDrawables { useSupportLibrary = true }
    }

    // v1 signing too, so sideloading works on OEM installers that still require it.
    // The Play upload key lives outside the project (../upload-key) and is never committed.
    val uploadProps = Properties().apply {
        val f = rootProject.file("../upload-key/keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        if (uploadProps.getProperty("storeFile") != null) {
            create("upload") {
                storeFile = file(uploadProps.getProperty("storeFile"))
                storePassword = uploadProps.getProperty("storePassword")
                keyAlias = uploadProps.getProperty("keyAlias")
                keyPassword = uploadProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    // "play": the Google Play build, updated by the Play Store only.
    // "github": the APK published on GitHub releases; it can update itself in the app.
    flavorDimensions += "store"
    productFlavors {
        create("play") { dimension = "store" }
        create("github") { dimension = "store" }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.window:window:1.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
