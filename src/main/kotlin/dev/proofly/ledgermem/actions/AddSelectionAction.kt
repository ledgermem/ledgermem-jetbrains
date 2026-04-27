package dev.proofly.ledgermem.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import dev.proofly.ledgermem.LedgerMemPlugin
import dev.proofly.ledgermem.services.LedgerMemService

class AddSelectionAction : AnAction("Add Selection to Memory", "Save selected text to LedgerMem", null) {
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
            Messages.showWarningDialog(project, "Select some text first.", LedgerMemPlugin.DISPLAY_NAME)
            return
        }
        val service = service<LedgerMemService>()
        val metadata = mapOf(
            "source" to "jetbrains",
            "file" to (file?.path ?: ""),
            "language" to (file?.fileType?.name ?: ""),
            "line" to (editor.document.getLineNumber(editor.selectionModel.selectionStart) + 1).toString(),
        )

        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Adding to LedgerMem...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    val memory = service.add(selection, metadata)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "Saved memory ${memory.id.take(8)}.",
                            LedgerMemPlugin.DISPLAY_NAME,
                        )
                    }
                }
            },
        )
    }
}
