package com.miracle.ui.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import javax.swing.JPanel

/**
 * Jarvis 工具窗口的根面板，管理主聊天视图和设置覆盖层之间的切换。
 *
 * @param project 当前项目实例
 * @param parentDisposable 父级可释放资源
 */
class JarvisToolWindowPanel(
    private val project: Project,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()), Disposable {
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

    /**
     * 将纯文本追加到当前活跃标签页的输入框中。
     *
     * @param text 要追加的文本
     */
    fun appendToInput(text: String) {
        appendInsertion(ChatComposerInsertion.PlainText(text))
    }

    /**
     * 将插入内容应用到当前活跃标签页的输入框中。
     *
     * @param insertion 要应用的插入内容
     */
    fun appendInsertion(insertion: ChatComposerInsertion) {
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        activePanel.appendInsertion(insertion)
        activePanel.requestFocusForInput()
    }

    /**
     * 批量将插入内容应用到当前活跃标签页的输入框中。
     *
     * @param insertions 要应用的插入内容列表
     */
    fun appendInsertions(insertions: List<ChatComposerInsertion>) {
        if (insertions.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        insertions.forEach(activePanel::appendInsertion)
        activePanel.requestFocusForInput()
    }

    /**
     * 批量追加关联文件到当前活跃标签页的上下文中。
     *
     * @param paths 文件路径列表
     */
    fun appendAssociatedFiles(paths: List<String>) {
        if (paths.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        paths.forEach(activePanel::appendAssociatedFile)
        activePanel.requestFocusForInput()
    }

    /**
     * 批量追加代码选区到当前活跃标签页的上下文中。
     *
     * @param selections 代码选区列表
     */
    fun appendAssociatedCodeSelections(selections: List<AssociatedContextItem.AssociatedCodeSelection>) {
        if (selections.isEmpty()) return
        showMainPanel()
        val activePanel = tabbedPane.activeTabPanel() ?: tabbedPane.addNewTab()
        selections.forEach(activePanel::appendAssociatedCodeSelection)
        activePanel.requestFocusForInput()
    }

    /**
     * 刷新所有标签页中的模型列表。
     */
    fun refreshModels() {
        tabbedPane.refreshModels()
    }

    /**
     * 显示指定设置分区的覆盖面板。
     *
     * @param section 要显示的设置分区
     */
    internal fun showSettingsOverlay(section: JarvisSettingsSection) {
        settingsOverlay.showSection(section)
        contentLayout.show(contentContainer, CARD_SETTINGS)
    }

    /**
     * 隐藏设置覆盖面板，返回主聊天视图。
     */
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

    override fun dispose() {
        tabbedPane.dispose()
        if (project.isDisposed) return
        project.getService(JarvisToolWindowService::class.java).unbind(this)
    }

    companion object {
        private const val CARD_MAIN = "main"
        private const val CARD_SETTINGS = "settings"
        private val PANEL_BACKGROUND = JBColor(Color(247, 248, 250), Color(43, 45, 48))
    }
}
