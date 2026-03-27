package com.miracle.ui.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import javax.swing.JPanel

class JarvisToolWindowPanel(
    private val project: Project,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()) {
    private val contentLayout = CardLayout()
    private val contentContainer = JPanel(contentLayout)
    private val tabbedPane = JarvisToolWindowTabbedPane(project)
    private val settingsOverlay = JarvisSettingsOverlayPanel(
        project = project,
        parentDisposable = parentDisposable,
        onBack = ::hideSettingsOverlay,
        onModelsChanged = ::refreshModels,
    )

    init {
        isOpaque = true
        background = PANEL_BACKGROUND
        border = JBUI.Borders.empty()

        contentContainer.isOpaque = true
        contentContainer.background = PANEL_BACKGROUND
        contentContainer.add(createMainPanel(), CARD_MAIN)
        contentContainer.add(settingsOverlay, CARD_SETTINGS)

        add(contentContainer, BorderLayout.CENTER)
        contentLayout.show(contentContainer, CARD_MAIN)
    }

    fun appendToInput(text: String) {
        appendInsertion(ChatComposerInsertion.PlainText(text))
    }

    fun appendInsertion(insertion: ChatComposerInsertion) {
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        activePanel.appendInsertion(insertion)
        activePanel.requestFocusForInput()
    }

    fun appendInsertions(insertions: List<ChatComposerInsertion>) {
        if (insertions.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        insertions.forEach(activePanel::appendInsertion)
        activePanel.requestFocusForInput()
    }

    fun appendAssociatedFiles(paths: List<String>) {
        if (paths.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        paths.forEach(activePanel::appendAssociatedFile)
        activePanel.requestFocusForInput()
    }

    fun appendAssociatedCodeSelections(selections: List<AssociatedContextItem.AssociatedCodeSelection>) {
        if (selections.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        selections.forEach(activePanel::appendAssociatedCodeSelection)
        activePanel.requestFocusForInput()
    }

    fun refreshModels() {
        tabbedPane.refreshModels()
    }

    internal fun showSettingsOverlay(section: JarvisSettingsSection) {
        settingsOverlay.showSection(section)
        contentLayout.show(contentContainer, CARD_SETTINGS)
    }

    fun hideSettingsOverlay() {
        showMainPanel()
    }

    private fun showMainPanel() {
        contentLayout.show(contentContainer, CARD_MAIN)
    }

    private fun createMainPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PANEL_BACKGROUND
            border = JBUI.Borders.empty()
            add(tabbedPane, BorderLayout.CENTER)
        }
    }

    companion object {
        private const val CARD_MAIN = "main"
        private const val CARD_SETTINGS = "settings"
        private val PANEL_BACKGROUND = JBColor(Color(247, 248, 250), Color(43, 45, 48))
    }
}
