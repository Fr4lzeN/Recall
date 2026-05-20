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
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(libs.androidx.junit)
}
