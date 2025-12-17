plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}

android {
    namespace = "lat.agrimet.agrimet"
    compileSdk = 36

    defaultConfig {
        applicationId = "lat.agrimet.agrimet"
        minSdk = 21
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Adding dependences for the asynchronous TCP connexion
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4") // OK con 1.9.x
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.ktor:ktor-network:2.3.12") // <-- bajar a 2.x

    // OkHttp para hacer requests HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Para usar JSONObject sin problemas
    //noinspection DuplicatePlatformClasses
    implementation("org.json:json:20240303")




    // 1. GSON (Para SerializedName y JSON) - ¡OBLIGATORIO!
    implementation("com.google.code.gson:gson:2.10.1")

    // 2. CORRUTINAS (Aunque ya tengas algunas, estas aseguran las últimas versiones ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 3. FCM (Firebase Messaging)
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

    // 4. OKHTTP (El cliente HTTP que ya usas)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 5. WorkManager (Aunque no lo usemos ahora, lo necesitas para FCM futuro)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

}