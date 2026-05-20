plugins {
    alias(libs.plugins.recall.android.library)
    alias(libs.plugins.recall.hilt)
}

android {
    namespace = "com.recall.app.core.media"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
