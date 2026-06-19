import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String, defaultValue: String? = null): String? =
    localProperties.getProperty(name) ?: defaultValue

fun escapedBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val debugAdmobAppId =
    localProperty("debug.admob.app.id", "ca-app-pub-3940256099942544~3347511713")!!
val debugAdmobBannerUnitId =
    localProperty("debug.admob.banner.unit.id", "ca-app-pub-3940256099942544/9214589741")!!
val debugAdmobInterstitialUnitId =
    localProperty("debug.admob.interstitial.unit.id", "ca-app-pub-3940256099942544/1033173712")!!
val debugAdmobRewardedUnitId =
    localProperty("debug.admob.rewarded.unit.id", "ca-app-pub-3940256099942544/5224354917")!!

val releaseAdmobAppId =
    localProperty("release.admob.app.id", localProperty("admob.app.id", debugAdmobAppId))!!
val releaseAdmobBannerUnitId =
    localProperty(
        "release.admob.banner.unit.id",
        localProperty("admob.banner.unit.id", debugAdmobBannerUnitId)
    )!!
val releaseAdmobInterstitialUnitId =
    localProperty(
        "release.admob.interstitial.unit.id",
        localProperty("admob.interstitial.unit.id", debugAdmobInterstitialUnitId)
    )!!
val releaseAdmobRewardedUnitId =
    localProperty(
        "release.admob.rewarded.unit.id",
        localProperty("admob.rewarded.unit.id", debugAdmobRewardedUnitId)
    )!!

val releaseStoreFile = localProperty("release.store.file")
val releaseStorePassword = localProperty("release.store.password")
val releaseKeyAlias = localProperty("release.key.alias")
val releaseKeyPassword = localProperty("release.key.password")
val hasReleaseSigning =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
        .all { !it.isNullOrBlank() }

android {
    namespace = "com.puneeth50.ailinguakit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.puneeth50.ailinguakit"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", debugAdmobAppId)
            buildConfigField("String", "ADMOB_APP_ID", escapedBuildConfigString(debugAdmobAppId))
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                escapedBuildConfigString(debugAdmobBannerUnitId)
            )
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                escapedBuildConfigString(debugAdmobInterstitialUnitId)
            )
            buildConfigField(
                "String",
                "ADMOB_REWARDED_UNIT_ID",
                escapedBuildConfigString(debugAdmobRewardedUnitId)
            )
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            resValue("string", "admob_app_id", releaseAdmobAppId)
            buildConfigField("String", "ADMOB_APP_ID", escapedBuildConfigString(releaseAdmobAppId))
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                escapedBuildConfigString(releaseAdmobBannerUnitId)
            )
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                escapedBuildConfigString(releaseAdmobInterstitialUnitId)
            )
            buildConfigField(
                "String",
                "ADMOB_REWARDED_UNIT_ID",
                escapedBuildConfigString(releaseAdmobRewardedUnitId)
            )
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ML Kit
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.language.id)

    // Coil
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Accompanist permissions
    implementation(libs.accompanist.permissions)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Billing + Ads
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    implementation("com.google.guava:guava:33.3.1-android")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    debugImplementation(libs.androidx.ui.tooling)
}
