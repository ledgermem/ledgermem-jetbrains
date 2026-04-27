package dev.proofly.ledgermem

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.project.Project

object LedgerMemPlugin {
    const val PLUGIN_ID: String = "dev.proofly.ledgermem"
    const val DISPLAY_NAME: String = "LedgerMem"
    val log: Logger = Logger.getInstance(LedgerMemPlugin::class.java)
}

@Service(Service.Level.PROJECT)
class LedgerMemStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        LedgerMemPlugin.log.info("LedgerMem ready for project: ${project.name}")
    }
}
