package com.qihoo.finance.lowcode.aiquestion.ui.search.renderer

/**
 * PresentationUtilKt
 *
 * @author fengjinfu-jk
 * date 2024/8/13
 * @version 1.0.0
 * @apiNote PresentationUtilKt
 */
import java.awt.Graphics2D
import java.awt.Point

fun Graphics2D.withTranslated(x: Int, y: Int, block: () -> Unit) {
    try {
        translate(x, y)
        block()
    } finally {
        translate(-x, -y)
    }
}

fun Point.translateNew(dx: Int, dy: Int) : Point = Point(x + dx, y + dy)
