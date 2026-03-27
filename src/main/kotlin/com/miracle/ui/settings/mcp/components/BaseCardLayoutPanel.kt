package com.miracle.ui.settings.mcp.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * 基于 CardLayout 的面板基类，提供加载、内容、消息三种状态的切换。
 *
 * 子类需要：
 * 1. 调用 [initCardLayout] 初始化布局
 * 2. 实现 [createContentPanel] 创建内容视图
 * 3. 使用 [showLoading]、[showMessage]、[showContent] 切换状态
 */
abstract class BaseCardLayoutPanel : JPanel(BorderLayout()) {

    private val cardLayout = CardLayout()
    protected val stackPanel = JPanel(cardLayout).apply {
        isOpaque = false
    }

    protected val loadingLabel = JBLabel("", SwingConstants.CENTER).apply {
        font = JBFont.label().biggerOn(1f)
    }

    protected val messageLabel = JBLabel("", SwingConstants.CENTER).apply {
        font = JBFont.label().biggerOn(1f)
        foreground = JBColor.GRAY
    }

    /**
     * 初始化卡片布局，子类应在构造完成后调用
     */
    protected fun initCardLayout() {
        isOpaque = false

        val loadingPanel = wrapCentered(loadingLabel)
        val messagePanel = wrapCentered(messageLabel)
        val contentPanel = createContentPanel()

        stackPanel.add(loadingPanel, STATE_LOADING)
        stackPanel.add(contentPanel, STATE_CONTENT)
        stackPanel.add(messagePanel, STATE_MESSAGE)

        add(stackPanel, BorderLayout.CENTER)
    }

    /**
     * 创建内容面板，子类必须实现
     */
    protected abstract fun createContentPanel(): JComponent

    /**
     * 显示加载状态
     */
    protected fun showLoading(message: String) {
        loadingLabel.text = message
        cardLayout.show(stackPanel, STATE_LOADING)
    }

    /**
     * 显示消息状态（用于错误或空状态提示）
     */
    protected fun showMessage(message: String) {
        messageLabel.text = message
        cardLayout.show(stackPanel, STATE_MESSAGE)
    }

    /**
     * 显示内容状态
     */
    protected fun showContent() {
        cardLayout.show(stackPanel, STATE_CONTENT)
    }

    /**
     * 将组件居中包装
     */
    protected open fun wrapCentered(component: JComponent): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(24, 12)
            add(component, BorderLayout.CENTER)
        }
    }

    companion object {
        const val STATE_LOADING = "loading"
        const val STATE_CONTENT = "content"
        const val STATE_MESSAGE = "message"
    }
}
