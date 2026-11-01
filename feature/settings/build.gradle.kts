plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.settings"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:ml"))
    implementation(project(":core:vector"))
    implementation(project(":core:worker"))
    implementation(libs.androidx.work.runtime.ktx)
}
