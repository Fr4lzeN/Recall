import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureAndroidLibrary() {
    pluginManager.apply("com.android.library")

    extensions.configure<LibraryExtension> {
        compileSdk = 36
        defaultConfig {
            minSdk = 28
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        testOptions {
            unitTests {
                isIncludeAndroidResources = true
            }
        }
    }
}

internal fun Project.configureAndroidApplication() {
    pluginManager.apply("com.android.application")

    extensions.configure<ApplicationExtension> {
        compileSdk = 36
        defaultConfig {
            minSdk = 28
            targetSdk = 36
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        buildTypes {
            release {
                isMinifyEnabled = false
            }
        }
    }
}
