plugins {
    alias(libs.plugins.recall.android.feature)
}

android {
    namespace = "com.recall.app.feature.timeline"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:media"))
}
