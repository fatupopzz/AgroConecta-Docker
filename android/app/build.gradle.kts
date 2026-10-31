plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// Firebase es opcional en entornos locales. Al agregar el archivo de
// configuración del proyecto, el plugin se activa sin cambiar este build.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
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
    .orElse("")
    .get()

val supportWhatsAppNumber = providers.gradleProperty("AGROCONECTA_SUPPORT_WHATSAPP")
    .orElse(providers.environmentVariable("AGROCONECTA_SUPPORT_WHATSAPP"))
    .orElse("")
    .get()
    .filter(Char::isDigit)

val pestMapStyleUrl = providers.gradleProperty("AGROCONECTA_PEST_MAP_STYLE_URL")
    .orElse(providers.environmentVariable("AGROCONECTA_PEST_MAP_STYLE_URL"))
    .orElse("https://tiles.openfreemap.org/styles/liberty")
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
        buildConfigField(
            "String",
            "SUPPORT_WHATSAPP_NUMBER",
            "\"$supportWhatsAppNumber\""
        )
        buildConfigField(
            "String",
            "PEST_MAP_STYLE_URL",
            "\"$pestMapStyleUrl\""
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            // R8 quita el código que no se usa; sin esto viajaban en el APK los
            // miles de íconos de material-icons-extended aunque se usen unos 60.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Las librerías nativas de TFLite para x86 solo sirven en
            // emuladores de computadora; en un teléfono real pesan 12 MB de más.
            // Debug las conserva para poder probar en cualquier emulador.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // TFLite abre el modelo con un mmap directo sobre el APK; si el archivo va
    // comprimido no se puede mapear y el clasificador falla al cargar.
    androidResources {
        noCompress += "tflite"
    }
}


dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.work.testing)
    testImplementation("androidx.compose.ui:ui-test-junit4:${libs.versions.composeUi.get()}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${libs.versions.composeUi.get()}")

    // Compose UI (versiones fijas, sin BOM)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime.livedata)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel integration
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.android)

    // Push notifications
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.work.runtime.ktx)

    // Mapa geográfico de alertas de plagas
    implementation(libs.maplibre.android)

    // Diagnóstico de plagas sin conexión (KAN-110)
    implementation(libs.litert)

    // Dependency injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Storage
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Image loading
    implementation(libs.coil.compose)

    // Instala los perfiles de arranque que ya traen Compose y las demás
    // librerías: la app abre más rápido aunque no se instale desde Play Store.
    implementation(libs.androidx.profileinstaller)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
}
