plugins {
    alias(libs.plugins.recall.android.library)
    alias(libs.plugins.recall.hilt)
}

android {
    namespace = "com.recall.app.core.worker"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:ml"))
    implementation(project(":core:vector"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.work.testing)
}
