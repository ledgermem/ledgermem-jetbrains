package dev.proofly.getmnemo.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import dev.proofly.getmnemo.MnemoPlugin
import dev.proofly.getmnemo.services.MnemoService
import dev.proofly.getmnemo.services.Memory

class SearchMemoryAction : AnAction("Search Memory", "Search Mnemo", null) {
    // IntelliJ 2022.3+ requires every AnAction to declare its update thread,
    // otherwise the platform logs "Action ... does not implement
    // getActionUpdateThread()" on every toolbar refresh. SearchMemoryAction
    // does not even override update(), so BGT is the safe default.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val query = Messages.showInputDialog(
            project,
            "Search query",
            "${MnemoPlugin.DISPLAY_NAME}: Search Memory",
            null,
        ) ?: return
        if (query.isBlank()) return

        val service = service<MnemoService>()
        ProgressManager().run(project, "Searching Mnemo...") {
            val results = service.search(query)
            ApplicationManager.getApplication().invokeLater {
                if (results.isEmpty()) {
                    Messages.showInfoMessage(project, "No matches found.", MnemoPlugin.DISPLAY_NAME)
                    return@invokeLater
                }
                showResults(results)
            }
        }
    }

    private fun showResults(results: List<Memory>) {
        val labels = results.map { previewLabel(it) }
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(labels)
            .setTitle("${MnemoPlugin.DISPLAY_NAME} (${results.size})")
            .setItemChosenCallback { selected ->
                val idx = labels.indexOf(selected)
                if (idx >= 0) {
                    Messages.showInfoMessage(results[idx].content, "Memory ${results[idx].id.take(8)}")
                }
            }
            .createPopup()
            .showInFocusCenter()
    }

    private fun previewLabel(memory: Memory): String {
        val first = memory.content.lineSequence().firstOrNull().orEmpty()
        val trimmed = if (first.length > 80) first.take(77) + "..." else first
        val score = memory.score?.let { String.format(" (%.2f)", it) }.orEmpty()
        return "$trimmed$score"
    }
}

private class ProgressManager {
    fun run(project: com.intellij.openapi.project.Project, title: String, block: () -> Unit) {
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, title, true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    block()
                }
            },
        )
    }
}
