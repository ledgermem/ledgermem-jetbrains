package dev.proofly.getmnemo

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

object MnemoPlugin {
    const val PLUGIN_ID: String = "dev.proofly.getmnemo"
    const val DISPLAY_NAME: String = "Mnemo"
    val log: Logger = Logger.getInstance(MnemoPlugin::class.java)
}

// Registered via plugin.xml as a postStartupActivity. ProjectActivity is the
// extension point — annotating with @Service was incorrect (it would force the
// platform to instantiate it as a service container instead of an extension).
class MnemoStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        MnemoPlugin.log.info("Mnemo ready for project: ${project.name}")
    }
}
