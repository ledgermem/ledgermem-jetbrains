package dev.proofly.ledgermem

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

object LedgerMemPlugin {
    const val PLUGIN_ID: String = "dev.proofly.ledgermem"
    const val DISPLAY_NAME: String = "LedgerMem"
    val log: Logger = Logger.getInstance(LedgerMemPlugin::class.java)
}

// Registered via plugin.xml as a postStartupActivity. ProjectActivity is the
// extension point — annotating with @Service was incorrect (it would force the
// platform to instantiate it as a service container instead of an extension).
class LedgerMemStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        LedgerMemPlugin.log.info("LedgerMem ready for project: ${project.name}")
    }
}
