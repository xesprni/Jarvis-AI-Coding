package com.miracle.ui.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.Icon

/**
 * Centralized color palette, icon resources and visual constants used by the chat UI.
 */
internal object ChatTheme {

    // ── Card layout names ─────────────────────────────────────────────
    const val CARD_WELCOME = "welcome"
    const val CARD_CHAT = "chat"

    // ── Backgrounds ───────────────────────────────────────────────────
    val PANEL_BACKGROUND: Color = JBColor(Color(247, 248, 250), Color(43, 45, 48))
    val CHAT_BACKGROUND: Color = PANEL_BACKGROUND
    val USER_MESSAGE_BACKGROUND: Color = JBColor(Color(252, 253, 255), Color(52, 54, 58))
    val INPUT_BACKGROUND: Color = JBColor(Color(251, 252, 255), Color(52, 54, 58))
    val ASSISTANT_BUBBLE: Color = JBColor(Color(247, 248, 250), Color(43, 45, 48))
    val USER_BUBBLE: Color = USER_MESSAGE_BACKGROUND
    val TOOL_BACKGROUND: Color = JBColor(Color(245, 245, 245), Color(45, 45, 45))
    val CODE_BACKGROUND: Color = JBColor(Color(245, 245, 245), Color(45, 45, 45))
    val TOOL_CONTENT_BACKGROUND: Color = JBColor(Color(245, 245, 245), Color(45, 45, 45))

    // ── Foregrounds ───────────────────────────────────────────────────
    val TOOL_TITLE_FOREGROUND: Color = JBColor(Color(45, 45, 45), Color(245, 245, 245))
    val MUTED_FOREGROUND: Color = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
    val USER_FOREGROUND: Color = JBColor(Color(0x1F, 0x5D, 0xE6), Color(0x9C, 0xBE, 0xFF))
    val SELECTOR_TEXT_FOREGROUND: Color = JBColor(Color(0x1F, 0x29, 0x38), Color(0xF2, 0xF5, 0xF8))

    // ── Error ─────────────────────────────────────────────────────────
    val ERROR_BACKGROUND: Color = JBColor(Color(0xFDECEA), Color(0x4A3434))
    val ERROR_FOREGROUND: Color = JBColor(Color(0x9F1D1D), Color(0xFFB4B4))

    // ── Borders / lines ───────────────────────────────────────────────
    val ROUNDED_BORDER_COLOR: Color = JBColor(Color(200, 200, 200), Color(70, 70, 70))
    val SPLIT_LINE_COLOR: Color = JBColor(Color(230, 238, 240), Color(30, 31, 34))
    val COMPOSER_BORDER_COLOR: Color = JBColor(Color(200, 210, 220), Color(65, 68, 72))

    // ── Dropdown ──────────────────────────────────────────────────────
    val DROPDOWN_BACKGROUND: Color = JBColor(Color(250, 251, 253), Color(48, 50, 54))
    val DROPDOWN_BORDER_COLOR: Color = JBColor(Color(214, 222, 232), Color(78, 82, 90))
    val DROPDOWN_ROW_BACKGROUND: Color = JBColor(Color(244, 247, 251), Color(56, 59, 64))
    val DROPDOWN_ROW_HOVER_BACKGROUND: Color = JBColor(Color(236, 242, 250), Color(64, 68, 74))
    val DROPDOWN_ROW_BORDER_COLOR: Color = JBColor(Color(221, 228, 237), Color(82, 86, 94))

    // ── Gradient border for the composer input ────────────────────────
    val PRIMARY_BORDER_START: Color = JBColor(Color(0x7C, 0xB3, 0xFF), Color(0x79, 0x8F, 0xFF))
    val PRIMARY_BORDER_END: Color = JBColor(Color(0x39, 0xD1, 0xC3), Color(0x48, 0xC1, 0xD8))

    // ── Icons ─────────────────────────────────────────────────────────
    val JARVIS_ICON: Icon = iconOrFallback("/img/inner/logo_round24.svg", AllIcons.Nodes.Plugin)
    val WELCOME_LOGO: Icon = iconOrFallback("/img/inner/logo_round32.svg", AllIcons.Nodes.Plugin)
    val USER_ICON: Icon = iconOrFallback("/img/plugins/user.svg", AllIcons.Nodes.Plugin)
    val SEND_ICON: Icon = iconOrFallback("/img/inner/send.svg", AllIcons.Actions.MenuOpen)
    val SETTINGS_ICON: Icon = iconOrFallback("/img/plugins/setting.svg", AllIcons.Nodes.Plugin)

    // ── JSON formatter ────────────────────────────────────────────────
    val PRETTY_JSON = kotlinx.serialization.json.Json { prettyPrint = true }

    // ── Timestamp formatter ───────────────────────────────────────────
    val TIMESTAMP_FORMATTER: java.time.format.DateTimeFormatter =
        java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(java.time.ZoneId.systemDefault())

    fun iconOrFallback(path: String, fallback: Icon): Icon {
        return runCatching { IconLoader.getIcon(path, JarvisChatTabPanel::class.java) }.getOrDefault(fallback)
    }
}
