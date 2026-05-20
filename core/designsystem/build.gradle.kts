plugins {
    alias(libs.plugins.recall.android.library)
    alias(libs.plugins.recall.android.compose)
}

android {
    namespace = "com.recall.app.core.designsystem"
}

dependencies {
    implementation(project(":core:common"))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
