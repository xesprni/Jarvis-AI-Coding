package com.qifu.ui.smartconversation.panels

import com.intellij.icons.AllIcons.Actions
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qihoo.finance.lowcode.smartconversation.actions.CopyAction
import com.qihoo.finance.lowcode.smartconversation.actions.IconActionButton
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BaseMessagePanel : BorderLayoutPanel() {

    private val headerPanel = BorderLayoutPanel()
    private val iconsWrapper = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
    protected val body: Body = Body()

    init {
        setupUI()
    }

    protected abstract fun createDisplayNameLabel(): JBLabel

    protected fun addIconActionButton(iconActionButton: IconActionButton) {
        if (iconsWrapper.components.isNotEmpty()) {
            iconsWrapper.add(Box.createHorizontalStrut(8))
        }
        iconsWrapper.add(iconActionButton)
    }

    fun enableAllActions(enabled: Boolean) {
        iconsWrapper.components.forEach { it.isEnabled = enabled }
    }

    fun disableActions(actionCodes: List<String>) {
        iconsWrapper.components
            .filterIsInstance<ActionButton>()
            .filter { actionCodes.contains(it.getClientProperty("actionCode")) }
            .forEach { it.isEnabled = false }
    }

    fun setActionEnabled(actionCode: String, enabled: Boolean) {
        iconsWrapper.components
            .filterIsInstance<ActionButton>()
            .filter { actionCode == it.getClientProperty("actionCode") }
            .forEach { it.isEnabled = enabled }
    }

    fun addCopyAction(onCopy: Runnable) {
        addIconActionButton(
            IconActionButton(
                object : AnAction(
                    "Copy Message Contents",
                    "Copy to clipboard",
                    Actions.Copy
                ) {
                    override fun actionPerformed(event: AnActionEvent) {
                        onCopy.run()
                        CopyAction.showCopyBalloon(event)
                    }
                },
                "COPY"
            )
        )
    }

    fun addContent(content: JComponent) {
        body.addContent(content)
    }

    fun updateContent(content: JComponent) {
        body.updateContent(content)
    }

    fun getContent(): JComponent = body.content

    private fun setupUI() {
        headerPanel.apply {
            isOpaque = false
            border = JBUI.Borders.empty(12, 8, 4, 8)

            this.addToLeft(createDisplayNameLabel())
            this.addToRight(iconsWrapper)
        }
        iconsWrapper.apply {
            isOpaque = false
        }

        addToTop(headerPanel)
        addToCenter(body)
    }

    protected class Body : BorderLayoutPanel() {
        var content: JComponent = BorderLayoutPanel()
            private set

        init {
            border = JBUI.Borders.empty(4, 8, 8, 8)
            isOpaque = false
        }

        fun addContent(component: JComponent) {
            content = component
            addToCenter(component)
        }

        fun updateContent(component: JComponent) {
            removeAll()
            revalidate()
            repaint()
            addContent(component)
        }
    }
}
