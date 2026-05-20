import org.gradle.api.Plugin
import org.gradle.api.Project

class RecallAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureAndroidCompose()
        }
    }
}
