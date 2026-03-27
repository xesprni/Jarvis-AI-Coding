package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelListener
import java.util.LinkedHashMap
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager

class JarvisToolWindowTabbedPane(
    private val project: Project,
) : JPanel(BorderLayout()), Disposable {

    private val activeTabs = LinkedHashMap<String, TabInfo>()
    private val contentLayout = BorderLayout()
    private val contentPanel = JPanel(contentLayout)
    private val tabsRow = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(0, 4)
    }
    private val tabsScrollPane = JScrollPane(tabsRow).apply {
        border = JBUI.Borders.empty()
        isOpaque = false
        viewport.isOpaque = false
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
        horizontalScrollBar.unitIncrement = JBUI.scale(24)
    }

    private var selectedTabId: String? = null

    init {
        isOpaque = true
        background = PANEL_BACKGROUND

        add(createTopBar(), BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        installHorizontalScroll()
        addNewTab()
    }

    fun activeTabPanel(): JarvisChatTabPanel? {
        val tabId = selectedTabId ?: return null
        return activeTabs[tabId]?.panel
    }

    fun addNewTab(initialConversationId: String? = null, title: String = ""): JarvisChatTabPanel {
        val tabId = UUID.randomUUID().toString().replace("-", "")
        val resolvedTitle = title.ifBlank { nextTitle() }
        val panel = JarvisChatTabPanel(
            project = project,
            tabId = tabId,
            initialConversationId = initialConversationId,
            onTitleChanged = { renameTab(tabId, it) },
            onOpenConversation = ::openConversationInNewTab,
            onArchiveConversation = { archiveTabAndOpenNew(tabId) },
        )

        val tabComponent = createTabComponent(tabId, resolvedTitle)
        activeTabs[tabId] = TabInfo(title = resolvedTitle, panel = panel, tabComponent = tabComponent)
        rebuildTabsRow()
        selectTab(tabId)
        panel.requestFocusForInput()
        return panel
    }

    fun refreshModels() {
        activeTabs.values.forEach { it.panel.refreshModels() }
    }

    fun openConversationInNewTab(conversationId: String, title: String) {
        addNewTab(initialConversationId = conversationId, title = title)
    }

    private fun archiveTabAndOpenNew(tabId: String) {
        val removed = removeTab(tabId) ?: run {
            addNewTab()
            return
        }
        Disposer.dispose(removed.panel)
        addNewTab()
    }

    fun renameTab(tabId: String, title: String) {
        val info = activeTabs[tabId] ?: return
        info.title = ensureUniqueName(title, tabId)
        updateTabComponent(info)
    }

    private fun createTopBar(): JComponent {
        lateinit var historyButton: JButton
        lateinit var settingsButton: JButton
        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(createTopIconButton(icon = AllIcons.General.Add, tooltip = "新会话") {
                addNewTab()
            })
            historyButton = createTopIconButton(icon = AllIcons.Vcs.History, tooltip = "历史会话") {
                val panel = activeTabPanel() ?: activeTabs.values.firstOrNull()?.panel ?: return@createTopIconButton
                panel.showHistoryPopup(historyButton)
            }
            add(historyButton)
            settingsButton = createTopIconButton(icon = ChatTheme.SETTINGS_ICON, tooltip = "设置") {
                SettingsMenuPopupBuilder(project).show(settingsButton)
            }
            add(settingsButton)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = UIManager.getColor("EditorTabs.background") ?: PANEL_BACKGROUND
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border())
            add(tabsScrollPane, BorderLayout.CENTER)
            add(actionsPanel, BorderLayout.EAST)
        }
    }

    private fun createTopIconButton(text: String? = null, icon: javax.swing.Icon? = null, tooltip: String, action: () -> Unit): JButton {
        return JButton(text, icon).apply {
            isOpaque = false
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            border = JBUI.Borders.empty(4)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = tooltip
            val s = JBUI.scale(24)
            preferredSize = Dimension(s, s)
            maximumSize = Dimension(s, s)
            minimumSize = Dimension(s, s)
            addActionListener { action() }
        }
    }

    private fun createTabComponent(tabId: String, title: String): JPanel {
        val titleLabel = JLabel(shortTitle(title)).apply {
            font = JBFont.label()
            border = JBUI.Borders.empty()
        }
        val closeButton = JButton(AllIcons.Actions.Close).apply {
            isOpaque = false
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            border = JBUI.Borders.empty()
            val s = JBUI.scale(16)
            preferredSize = Dimension(s, s)
            maximumSize = Dimension(s, s)
            minimumSize = Dimension(s, s)
            rolloverIcon = AllIcons.Actions.CloseHovered
            toolTipText = "关闭会话"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { closeTab(tabId) }
        }
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(4, 6)
            add(titleLabel)
            add(Box.createHorizontalStrut(JBUI.scale(2)))
            add(closeButton)
            toolTipText = title
        }
        val clickListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                selectTab(tabId)
            }
        }
        panel.addMouseListener(clickListener)
        titleLabel.addMouseListener(clickListener)
        return panel
    }

    private fun updateTabComponent(info: TabInfo) {
        val label = info.tabComponent.components.firstOrNull() as? JLabel ?: return
        label.text = shortTitle(info.title)
        info.tabComponent.toolTipText = info.title
        styleTab(info.tabComponent, selected = selectedTabId == activeTabs.entries.firstOrNull { it.value == info }?.key)
    }

    private fun selectTab(tabId: String) {
        val info = activeTabs[tabId] ?: return
        selectedTabId = tabId
        contentPanel.removeAll()
        contentPanel.add(info.panel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
        activeTabs.forEach { (id, tabInfo) ->
            styleTab(tabInfo.tabComponent, selected = id == tabId)
        }
        SwingUtilities.invokeLater {
            info.tabComponent.scrollRectToVisible(info.tabComponent.bounds)
        }
    }

    private fun styleTab(component: JPanel, selected: Boolean) {
        component.isOpaque = selected
        component.background = if (selected) {
            UIManager.getColor("EditorTabs.selectedBackground") ?: TAB_SELECTED_BACKGROUND
        } else {
            UIManager.getColor("EditorTabs.background") ?: PANEL_BACKGROUND
        }
        component.border = if (selected) {
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0,
                    UIManager.getColor("EditorTabs.underlineColor") ?: TAB_SELECTED_BORDER),
                JBUI.Borders.empty(4, 6, 2, 6),
            )
        } else {
            JBUI.Borders.empty(4, 6)
        }
        val titleLabel = component.components.firstOrNull() as? JLabel
        titleLabel?.foreground = if (selected) {
            UIManager.getColor("EditorTabs.selectedForeground") ?: TAB_SELECTED_FOREGROUND
        } else {
            UIManager.getColor("EditorTabs.foreground") ?: TAB_FOREGROUND
        }
    }

    private fun closeTab(tabId: String) {
        val keys = activeTabs.keys.toList()
        val removedIndex = keys.indexOf(tabId)
        val info = removeTab(tabId) ?: return
        Disposer.dispose(info.panel)

        if (activeTabs.isEmpty()) {
            addNewTab()
            return
        }

        val fallbackIndex = (removedIndex - 1).coerceAtLeast(0).coerceAtMost(activeTabs.size - 1)
        val nextTabId = activeTabs.keys.elementAt(fallbackIndex)
        selectTab(nextTabId)
        tabsRow.revalidate()
        tabsRow.repaint()
    }

    private fun removeTab(tabId: String): TabInfo? {
        val info = activeTabs.remove(tabId) ?: return null
        if (selectedTabId == tabId) {
            selectedTabId = null
            contentPanel.removeAll()
            contentPanel.revalidate()
            contentPanel.repaint()
        }
        rebuildTabsRow()
        return info
    }

    override fun dispose() {
        activeTabs.values.map { it.panel }.forEach(Disposer::dispose)
        selectedTabId = null
        activeTabs.clear()
        contentPanel.removeAll()
        tabsRow.removeAll()
    }

    private fun rebuildTabsRow() {
        tabsRow.removeAll()
        activeTabs.values.forEach { info ->
            tabsRow.add(info.tabComponent)
        }
        tabsRow.revalidate()
        tabsRow.repaint()
    }

    private fun installHorizontalScroll() {
        val wheelListener = MouseWheelListener { event ->
            val horizontalBar = tabsScrollPane.horizontalScrollBar
            val delta = event.wheelRotation * horizontalBar.unitIncrement
            horizontalBar.value = horizontalBar.value + delta
            event.consume()
        }
        tabsScrollPane.addMouseWheelListener(wheelListener)
        tabsRow.addMouseWheelListener(wheelListener)
    }

    private fun nextTitle(): String {
        val existingIndexes = activeTabs.values.mapNotNull { info ->
            Regex("""Chat (\d+)""").matchEntire(info.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        val nextIndex = (existingIndexes.maxOrNull() ?: 0) + 1
        return "Chat $nextIndex"
    }

    private fun ensureUniqueName(title: String, tabId: String): String {
        val base = title.trim().ifBlank { nextTitle() }
        var candidate = base
        var suffix = 2
        while (activeTabs.any { (key, value) -> key != tabId && value.title == candidate }) {
            candidate = "$base ($suffix)"
            suffix += 1
        }
        return candidate
    }

    private fun shortTitle(title: String): String {
        val maxLength = 14
        return if (title.length <= maxLength) title else title.take(maxLength - 3) + "..."
    }

    private data class TabInfo(
        var title: String,
        val panel: JarvisChatTabPanel,
        val tabComponent: JPanel,
    )

    companion object {
        private val PANEL_BACKGROUND
            get() = JBColor.PanelBackground
        private val TAB_SELECTED_BACKGROUND
            get() = UIManager.getColor("EditorTabs.selectedBackground") ?: JBColor.background()
        private val TAB_SELECTED_BORDER
            get() = UIManager.getColor("EditorTabs.underlineColor") ?: JBColor.namedColor("Component.focusColor", JBColor(0x4083C9, 0x548AF7))
        private val TAB_FOREGROUND
            get() = UIManager.getColor("EditorTabs.foreground") ?: JBColor.foreground()
        private val TAB_SELECTED_FOREGROUND
            get() = UIManager.getColor("EditorTabs.selectedForeground") ?: JBColor.foreground()
    }
}
