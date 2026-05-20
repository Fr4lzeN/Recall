plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.search"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(project(":core:database"))
    implementation(project(":core:ml"))
    implementation(project(":core:vector"))
}
