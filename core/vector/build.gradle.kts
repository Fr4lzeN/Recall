plugins {
    alias(libs.plugins.recall.android.library)
}

android {
    namespace = "com.recall.app.core.vector"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
