plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.onboarding"
}

dependencies {
    implementation(project(":core:media"))
    implementation(project(":core:worker"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
