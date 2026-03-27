package com.miracle.ui.core

import com.intellij.util.ui.JBUI
import com.miracle.ui.core.ChatTheme.PANEL_BACKGROUND
import com.miracle.ui.core.ChatTheme.SPLIT_LINE_COLOR
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Assembles the bottom prompt/composer panel that contains the input area,
 * model/mode selectors, send/stop buttons, and the ask panel.
 */
internal object ChatPromptPanel {

    /**
     * @param inputArea      the text area where users type messages
     * @param askPanel       the panel shown when the AI asks a question
     * @param modelComboBox  model selector dropdown
     * @param chatModeComboBox  chat mode selector dropdown
     * @param sendButton     send action button
     * @param stopButton     stop action button
     */
    fun create(
        inputComponent: JComponent,
        headerComponent: JComponent,
        askPanel: AskPanel,
        modelComboBox: JComboBox<*>,
        chatModeComboBox: JComboBox<*>,
        sendButton: JButton,
        stopButton: JButton,
    ): JComponent {
        val leftToolbarPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(modelComboBox)
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(createToolbarSeparator())
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(chatModeComboBox)
        }

        val rightButtonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(sendButton)
            add(Box.createHorizontalStrut(JBUI.scale(4)))
            add(stopButton)
            val buttonWidth = sendButton.preferredSize.width + stopButton.preferredSize.width + JBUI.scale(8)
            val buttonHeight = maxOf(sendButton.preferredSize.height, stopButton.preferredSize.height)
            minimumSize = Dimension(buttonWidth, buttonHeight)
            preferredSize = Dimension(buttonWidth, buttonHeight)
        }

        val footerRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(SPLIT_LINE_COLOR, 1, 0, 0, 0),
                JBUI.Borders.empty(8, 12, 10, 12),
            )
            add(leftToolbarPanel, BorderLayout.CENTER)
            add(rightButtonPanel, BorderLayout.EAST)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = PANEL_BACKGROUND
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(SPLIT_LINE_COLOR, 1, 0, 0, 0),
                JBUI.Borders.empty(8, 14, 14, 14),
            )
            add(askPanel, BorderLayout.NORTH)
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.emptyTop(askPanel.spacingTop())
                add(RoundedComposerPanel().apply {
                    add(headerComponent, BorderLayout.NORTH)
                    add(inputComponent, BorderLayout.CENTER)
                    add(footerRow, BorderLayout.SOUTH)
                }, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
    }
}
