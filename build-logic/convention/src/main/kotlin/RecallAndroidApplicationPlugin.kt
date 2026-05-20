import org.gradle.api.Plugin
import org.gradle.api.Project

class RecallAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureAndroidApplication()
            pluginManager.apply("recall.android.compose")
        }
    }
}
