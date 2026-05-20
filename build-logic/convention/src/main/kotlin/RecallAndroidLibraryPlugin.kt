import org.gradle.api.Plugin
import org.gradle.api.Project

class RecallAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureAndroidLibrary()
        }
    }
}
