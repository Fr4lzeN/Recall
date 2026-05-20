plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.detail"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(project(":core:database"))
    implementation(project(":core:media"))
}
