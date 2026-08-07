plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.rocat.scripting.rhino"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(projects.scripting.api)
    implementation(projects.core.common)
    implementation(libs.rhino)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.serialization)
}