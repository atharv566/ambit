import java.util.Properties
import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
// AGP 9 supplies Kotlin support; do not also apply kotlin-android.
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun apiUrl(property: String, placeholder: String, release: Boolean): String {
    val value = providers.gradleProperty(property).orNull ?: local.getProperty(property) ?: placeholder
    val uri = URI(value)
    require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.path.endsWith("/api/v1/")) { "$property must be a URL ending in /api/v1/ without credentials, query or fragment." }
    require(uri.scheme == "https" || (!release && uri.scheme == "http")) { "$property requires HTTPS for release." }
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
android {
    namespace = "com.example.cpvts"
    buildToolsVersion = "37.0.0"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "com.example.cpvts"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", apiUrl("CPVTS_DEV_API_BASE_URL", "https://cpvts-dev-api-host.invalid/api/v1/", false))
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", apiUrl("CPVTS_PROD_API_BASE_URL", "https://cpvts-production-api-host.invalid/api/v1/", true))
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = false }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}
