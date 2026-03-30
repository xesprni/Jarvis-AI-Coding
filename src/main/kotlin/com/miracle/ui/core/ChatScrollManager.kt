package com.miracle.ui.core

import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.Scrollable
import javax.swing.SwingUtilities
import java.awt.BorderLayout

/**
 * 视口快照数据类，保存滚动位置信息。
 *
 * @property point 视口位置点
 */
// ── Data class to snapshot viewport state ────────────────────────────
internal data class ViewportSnapshot(val point: Point)

/**
 * Manages smart scroll behavior for the chat message pane:
 *  - auto-follow when the user is at the bottom
 *  - preserve reading position when the user scrolls up
 *  - bridge nested scroll panes with the outer message scroll pane
 */
internal class ChatScrollManager(
    private val messageScrollPane: JScrollPane,
    private val messageContainer: JPanel,
) {
    /** 是否跟随最新输出自动滚动到底部 */
    var followLatestOutput = true
    /** 是否为程序化触发的滚动（非用户手动操作） */
    var programmaticScroll = false

    /** Install adjustment and resize listeners on the scroll pane. */
    fun install() {
        messageScrollPane.verticalScrollBar.addAdjustmentListener {
            if (programmaticScroll) return@addAdjustmentListener
            followLatestOutput = isNearBottom()
        }
        messageScrollPane.viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                val snapshot = captureViewportSnapshot()
                messageContainer.revalidate()
                messageContainer.repaint()
                restoreViewportSnapshot(snapshot)
            }
        })
    }

    /**
     * 检查当前滚动位置是否接近底部。
     *
     * @return 如果接近底部返回 true
     */
    fun isNearBottom(): Boolean {
        val vertical = messageScrollPane.verticalScrollBar
        return vertical.value + vertical.model.extent >= vertical.maximum - JBUI.scale(72)
    }

    /**
     * 滚动到底部，可选择是否强制滚动。
     *
     * @param force 是否强制滚动（忽略自动跟随状态），默认 false
     */
    fun scrollToBottom(force: Boolean = false) {
        if (!force && !followLatestOutput) return
        if (force) followLatestOutput = true
        SwingUtilities.invokeLater {
            if (!force && !followLatestOutput) return@invokeLater
            val vertical = messageScrollPane.verticalScrollBar
            programmaticScroll = true
            vertical.value = vertical.maximum
            SwingUtilities.invokeLater {
                vertical.value = vertical.maximum
                programmaticScroll = false
            }
        }
    }

    /**
     * 捕获当前视口的滚动位置快照。
     *
     * @return 视口快照，如果处于跟随状态则返回 null
     */
    fun captureViewportSnapshot(): ViewportSnapshot? {
        if (followLatestOutput) return null
        val viewport = messageScrollPane.viewport
        return ViewportSnapshot(point = Point(viewport.viewPosition))
    }

    /**
     * 恢复之前捕获的视口快照位置。
     *
     * @param snapshot 要恢复的视口快照，为 null 时不执行操作
     */
    fun restoreViewportSnapshot(snapshot: ViewportSnapshot?) {
        if (snapshot == null) return
        programmaticScroll = true
        fun applyRestore() {
            val vertical = messageScrollPane.verticalScrollBar
            val maxY = maxOf(0, vertical.maximum - vertical.model.extent)
            val targetY = snapshot.point.y.coerceIn(0, maxY)
            messageScrollPane.viewport.viewPosition = Point(snapshot.point.x, targetY)
        }
        SwingUtilities.invokeLater {
            applyRestore()
            SwingUtilities.invokeLater {
                applyRestore()
                programmaticScroll = false
                followLatestOutput = isNearBottom()
            }
        }
    }

    /**
     * Wraps a component so its preferred width tracks the viewport width of the message scroll pane.
     */
    fun wrapForConversationWidth(content: javax.swing.JComponent): javax.swing.JComponent {
        return object : JPanel(BorderLayout()) {
            init {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                add(content, BorderLayout.CENTER)
            }

            override fun getPreferredSize(): Dimension {
                val base = super.getPreferredSize()
                val viewportWidth = messageScrollPane.viewport.extentSize.width - JBUI.scale(12)
                if (viewportWidth <= 0) return base
                return Dimension(maxOf(JBUI.scale(120), viewportWidth), base.height)
            }

            override fun getMaximumSize(): Dimension {
                return Dimension(Int.MAX_VALUE, preferredSize.height)
            }
        }
    }

    /**
     * Bridges scroll events between a nested scroll pane and the outer message scroll pane.
     */
    fun installNestedScrollBridge(
        scrollPane: JScrollPane,
        alwaysDelegateVerticalWheel: Boolean = false,
    ) {
        scrollPane.verticalScrollBar.unitIncrement = JBUI.scale(20)
        scrollPane.horizontalScrollBar.unitIncrement = JBUI.scale(20)

        val wheelHandler = MouseWheelListener { event ->
            if (event.isConsumed) return@MouseWheelListener
            if (event.isShiftDown) {
                val hBar = scrollPane.horizontalScrollBar
                if (hBar != null && hBar.isVisible) {
                    val delta = event.wheelRotation * hBar.unitIncrement
                    hBar.value = hBar.value + delta
                }
                event.consume()
                return@MouseWheelListener
            }

            if (alwaysDelegateVerticalWheel) {
                dispatchToOuterScrollPane(scrollPane, event)
                return@MouseWheelListener
            }

            val vBar = scrollPane.verticalScrollBar
            if (vBar == null || !vBar.isVisible) {
                dispatchToOuterScrollPane(scrollPane, event)
                return@MouseWheelListener
            }

            val atTop = vBar.value <= vBar.minimum
            val atBottom = vBar.value + vBar.model.extent >= vBar.maximum
            val scrollingUp = event.preciseWheelRotation < 0
            val scrollingDown = event.preciseWheelRotation > 0

            if ((scrollingUp && atTop) || (scrollingDown && atBottom)) {
                dispatchToOuterScrollPane(scrollPane, event)
            } else {
                val delta = event.wheelRotation * vBar.unitIncrement
                vBar.value = vBar.value + delta
                event.consume()
            }
        }

        scrollPane.isWheelScrollingEnabled = false
        installWheelListenerRecursively(scrollPane, wheelHandler)
    }

    private fun dispatchToOuterScrollPane(source: Component, event: MouseWheelEvent) {
        val point = SwingUtilities.convertPoint(source, event.point, messageScrollPane)
        messageScrollPane.dispatchEvent(
            MouseWheelEvent(
                messageScrollPane, event.id, event.`when`, event.modifiersEx,
                point.x, point.y, event.xOnScreen, event.yOnScreen,
                event.clickCount, event.isPopupTrigger, event.scrollType,
                event.scrollAmount, event.wheelRotation, event.preciseWheelRotation,
            )
        )
        event.consume()
    }

    private fun installWheelListenerRecursively(component: Component, listener: MouseWheelListener) {
        component.addMouseWheelListener(listener)
        if (component is java.awt.Container) {
            for (child in component.components) {
                installWheelListenerRecursively(child, listener)
            }
        }
    }
}

/**
 * A JPanel implementing [Scrollable] to ensure the message column tracks viewport width.
 */
/**
 * 实现 Scrollable 接口的消息列表面板，确保面板宽度跟随视口宽度。
 */
internal class MessageColumnPanel : JPanel(), Scrollable {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize

    override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return JBUI.scale(28)
    }

    override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int {
        return maxOf(JBUI.scale(160), (visibleRect?.height ?: 0) - JBUI.scale(48))
    }

    override fun getScrollableTracksViewportWidth(): Boolean = true

    override fun getScrollableTracksViewportHeight(): Boolean = false
}
