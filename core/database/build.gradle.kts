plugins {
    alias(libs.plugins.recall.android.library)
    alias(libs.plugins.recall.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.recall.app.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
