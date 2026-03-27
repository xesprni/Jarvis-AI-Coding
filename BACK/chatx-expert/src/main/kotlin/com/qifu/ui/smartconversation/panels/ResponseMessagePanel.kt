package com.qifu.ui.smartconversation.panels

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.SwingConstants

open class ResponseMessagePanel : BaseMessagePanel() {

    init {
        background = Constants.Color.PANEL_BACKGROUND
    }

    override fun createDisplayNameLabel(): JBLabel {
        return JBLabel(
            "Jarvis",
            Icons.LOGO_ROUND24,
            SwingConstants.LEADING
        )
            .setAllowAutoWrapping(true)
            .withFont(JBFont.label().asBold())
            .apply {
                iconTextGap = 6
            }
    }
}