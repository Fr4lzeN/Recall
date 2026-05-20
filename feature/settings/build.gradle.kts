plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.settings"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(project(":core:database"))
    implementation(project(":core:ml"))
}
