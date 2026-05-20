plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.timeline"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:worker"))
}
