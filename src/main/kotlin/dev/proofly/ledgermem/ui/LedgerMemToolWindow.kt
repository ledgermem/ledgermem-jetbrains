package dev.proofly.ledgermem.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import dev.proofly.ledgermem.services.LedgerMemService
import dev.proofly.ledgermem.services.Memory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import java.awt.BorderLayout
import java.awt.FlowLayout

class LedgerMemToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = LedgerMemPanel(project)
        val content = toolWindow.contentManager.factory.createContent(panel.component, "Recent", false)
        toolWindow.contentManager.addContent(content)
        panel.refresh()
    }
}

class LedgerMemPanel(private val project: Project? = null) {
    private val model = DefaultListModel<String>()
    private val list = JBList(model)
    private val refreshBtn = JButton("Refresh")
    private val deleteBtn = JButton("Delete selected")
    private var memories: List<Memory> = emptyList()

    val component: JPanel = JPanel(BorderLayout()).apply {
        add(JBScrollPane(list), BorderLayout.CENTER)
        add(toolbar(), BorderLayout.NORTH)
    }

    init {
        list.setEmptyText("No memories yet — use the Add Selection action.")
        refreshBtn.addActionListener { refresh() }
        deleteBtn.addActionListener { deleteSelected() }
    }

    private fun toolbar(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(refreshBtn)
        add(deleteBtn)
    }

    fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val service = service<LedgerMemService>()
            val items = runCatching { service.recent() }.getOrElse { emptyList() }
            ApplicationManager.getApplication().invokeLater {
                // Guard against the tool window being closed (project disposed) while
                // the pooled-thread fetch was in flight — without this we would mutate
                // the Swing model after dispose and leak listener references.
                if (project?.isDisposed == true) return@invokeLater
                memories = items
                model.clear()
                items.forEach { model.addElement(formatItem(it)) }
            }
        }
    }

    private fun deleteSelected() {
        val idx = list.selectedIndex
        if (idx < 0 || idx >= memories.size) return
        val target = memories[idx]
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching { service<LedgerMemService>().delete(target.id) }
            ApplicationManager.getApplication().invokeLater {
                if (project?.isDisposed == true) return@invokeLater
                refresh()
            }
        }
    }

    private fun formatItem(m: Memory): String {
        val first = m.content.lineSequence().firstOrNull().orEmpty()
        val preview = if (first.length > 60) first.take(57) + "..." else first
        return "$preview   [${m.id.take(8)}]"
    }

    @Suppress("unused")
    private val _alignment = SwingConstants.LEADING
}
