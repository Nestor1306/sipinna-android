plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.sipinnaapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sipinnaapp"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // Retrofit: hace las llamadas HTTP al backend Go
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp: lo usamos para mandar las fotos como archivo (multipart)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coroutines: para no bloquear la pantalla mientras espera respuesta
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Íconos de Material (flecha, lupa, +, x, etc.)
    implementation("androidx.compose.material:material-icons-extended")
    // Coil: muestra las fotos de la galería en pantalla
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Google Play Services: ubicación GPS del teléfono
    implementation("com.google.android.gms:play-services-location:21.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("com.mapbox.maps:android-ndk27:11.31.0")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.31.0")
}