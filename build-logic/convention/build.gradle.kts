plugins {
    `kotlin-dsl`
}

group = "com.recall.app.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "recall.android.library"
            implementationClass = "RecallAndroidLibraryPlugin"
        }
        register("androidFeature") {
            id = "recall.android.feature"
            implementationClass = "RecallAndroidFeaturePlugin"
        }
        register("androidApplication") {
            id = "recall.android.application"
            implementationClass = "RecallAndroidApplicationPlugin"
        }
        register("androidCompose") {
            id = "recall.android.compose"
            implementationClass = "RecallAndroidComposePlugin"
        }
        register("hilt") {
            id = "recall.hilt"
            implementationClass = "RecallHiltPlugin"
        }
    }
}
