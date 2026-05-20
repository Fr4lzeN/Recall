plugins {
    alias(libs.plugins.recall.android.library)
    alias(libs.plugins.recall.hilt)
}

android {
    namespace = "com.recall.app.core.ml"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.tensorflow.lite)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
