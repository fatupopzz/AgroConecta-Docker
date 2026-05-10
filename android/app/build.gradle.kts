plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val configuredApiBaseUrl = providers.gradleProperty("AGROCONECTA_API_BASE_URL")
    .orElse(providers.environmentVariable("AGROCONECTA_API_BASE_URL"))

val debugApiBaseUrl = configuredApiBaseUrl
    .orElse("http://10.0.2.2:8080/api/")
    .get()

val releaseApiBaseUrl = configuredApiBaseUrl
    .map { url ->
        if (url.startsWith("http://")) {
            "https://${url.removePrefix("http://")}"
        } else {
            url
        }
    }
    .orElse("https://10.0.2.2:8080/api/")
    .get()

android {
    namespace = "com.uvg.agroconecta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uvg.agroconecta"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
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
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.android)
    implementation(libs.glide)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
}
