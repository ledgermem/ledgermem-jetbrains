package dev.proofly.getmnemo.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import dev.proofly.getmnemo.MnemoPlugin
import dev.proofly.getmnemo.services.MnemoService

class AddSelectionAction : AnAction("Add Selection to Memory", "Save selected text to Mnemo", null) {
    // Run update() on a background thread. The work it does — checking whether
    // an editor exists and has a selection — is read-safe and does not touch
    // any EDT-only UI state. Declaring EDT here meant every toolbar / popup
    // refresh blocked the UI thread, which is exactly what JetBrains' BGT
    // contract is meant to prevent.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor?.selectionModel?.hasSelection() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val selection = editor.selectionModel.selectedText.orEmpty().trim()
        if (selection.isEmpty()) {
            Messages.showWarningDialog(project, "Select some text first.", MnemoPlugin.DISPLAY_NAME)
            return
        }
        val service = service<MnemoService>()
        val metadata = mapOf(
            "source" to "jetbrains",
            "file" to (file?.path ?: ""),
            "language" to (file?.fileType?.name ?: ""),
            "line" to (editor.document.getLineNumber(editor.selectionModel.selectionStart) + 1).toString(),
        )

        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Adding to Mnemo...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    val memory = service.add(selection, metadata)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "Saved memory ${memory.id.take(8)}.",
                            MnemoPlugin.DISPLAY_NAME,
                        )
                    }
                }
            },
        )
    }
}
