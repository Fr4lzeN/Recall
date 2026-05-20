plugins {
    alias(libs.plugins.recall.android.library)
}

android {
    namespace = "com.recall.app.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
