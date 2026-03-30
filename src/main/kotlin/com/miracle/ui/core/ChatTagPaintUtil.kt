package com.miracle.ui.core

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent

/**
 * 聊天标签绘制工具，提供圆角背景和边框的绘制方法。
 */
internal object ChatTagPaintUtil {
    /** 圆角宽度 */
    private const val ARC_WIDTH = 8f
    /** 圆角高度 */
    private const val ARC_HEIGHT = 8f
    /** 边框线宽 */
    private const val STROKE_WIDTH = 1f

    /**
     * 在指定组件上绘制圆角背景和边框，选中时使用实线，未选中时使用虚线。
     *
     * @param g 图形上下文
     * @param component 目标组件
     * @param selected 是否选中状态，默认 true
     */
    fun drawRoundedBackground(
        g: Graphics,
        component: JComponent,
        selected: Boolean = true,
    ) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val rect = RoundRectangle2D.Float(
                STROKE_WIDTH / 2f,
                STROKE_WIDTH / 2f,
                component.width.toFloat() - STROKE_WIDTH,
                component.height.toFloat() - STROKE_WIDTH,
                ARC_WIDTH,
                ARC_HEIGHT,
            )
            g2.color = service<EditorColorsManager>().globalScheme.defaultBackground
            g2.fill(rect)
            if (!selected) {
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
            }
            g2.color = JBUI.CurrentTheme.Focus.defaultButtonColor()
            g2.stroke = if (selected) {
                BasicStroke(STROKE_WIDTH)
            } else {
                BasicStroke(
                    STROKE_WIDTH,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,
                    0f,
                    floatArrayOf(10f, 5f),
                    0f,
                )
            }
            g2.draw(rect)
        } finally {
            g2.dispose()
        }
    }
}
