plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.onboarding"
}

dependencies {
    implementation(project(":core:media"))
}
