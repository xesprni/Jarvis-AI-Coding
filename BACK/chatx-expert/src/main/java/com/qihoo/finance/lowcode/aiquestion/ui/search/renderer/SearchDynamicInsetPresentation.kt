package com.qihoo.finance.lowcode.aiquestion.ui.search.renderer

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * DynamicInsetPresentation
 *
 * @author fengjinfu-jk
 * date 2024/8/13
 * @version 1.0.0
 * @apiNote DynamicInsetPresentation
 */
class SearchDynamicInsetPresentation(
    presentation: InlayPresentation,
    val valueProvider: SearchInsetValueProvider
) : StaticDelegatePresentation(presentation) {
    private var isPresentationUnderCursor = false

    private val left: Int
        get() = valueProvider.left
    private val right: Int
        get() = valueProvider.right
    private val top: Int
        get() = valueProvider.top
    private val down: Int
        get() = valueProvider.down

    override val width: Int
        get() = presentation.width + left + right
    override val height: Int
        get() = presentation.height + top + down

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        g.withTranslated(left, top) {
            presentation.paint(g, attributes)
        }
    }

    private fun handleMouse(
        original: Point,
        action: (InlayPresentation, Point) -> Unit
    ) {
        val x = original.x
        val y = original.y
        val cursorIsOutOfBounds = x < left || x >= left + presentation.width || y < top || y >= top + presentation.height
        if (cursorIsOutOfBounds) {
            if (isPresentationUnderCursor) {
                presentation.mouseExited()
                isPresentationUnderCursor = false
            }
            return
        }
        val translated = original.translateNew(-left, -top)
        action(presentation, translated)
    }

    override fun mouseClicked(event: MouseEvent, translated: Point) {
        handleMouse(translated) { presentation, point ->
            presentation.mouseClicked(event, point)
        }
    }

    override fun mouseMoved(event: MouseEvent, translated: Point) {
        handleMouse(translated) { presentation, point ->
            presentation.mouseMoved(event, point)
        }
    }

    override fun mouseExited() {
        presentation.mouseExited()
        isPresentationUnderCursor = false
    }
}
