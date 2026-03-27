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
    val PLAN_CARD_BACKGROUND: Color = JBColor(Color(244, 247, 255), Color(48, 52, 59))
    val PLAN_CARD_HEADER_BACKGROUND: Color = JBColor(Color(234, 241, 255), Color(54, 59, 68))
    val PLAN_CARD_SURFACE_BACKGROUND: Color = JBColor(Color(252, 253, 255), Color(43, 45, 48))

    // ── Foregrounds ───────────────────────────────────────────────────
    val TOOL_TITLE_FOREGROUND: Color = JBColor(Color(45, 45, 45), Color(245, 245, 245))
    val MUTED_FOREGROUND: Color = JBColor(Color(0x6B, 0x75, 0x86), Color(0xA0, 0xA8, 0xB8))
    val USER_FOREGROUND: Color = JBColor(Color(0x1F, 0x5D, 0xE6), Color(0x9C, 0xBE, 0xFF))
    val SELECTOR_TEXT_FOREGROUND: Color = JBColor(Color(0x1F, 0x29, 0x38), Color(0xF2, 0xF5, 0xF8))
    val SELECTOR_HOVER_BACKGROUND: Color = JBColor(Color(0xEF, 0xF1, 0xF4), Color(0x39, 0x3E, 0x46))
    val SELECTOR_PRESSED_BACKGROUND: Color = JBColor(Color(0xE4, 0xE8, 0xED), Color(0x33, 0x38, 0x40))
    val SELECTOR_HOVER_BORDER: Color = JBColor(Color(0xD7, 0xDE, 0xE7), Color(0x52, 0x5A, 0x64))
    val SELECTOR_PRESSED_BORDER: Color = JBColor(Color(0xC6, 0xCF, 0xDA), Color(0x62, 0x6B, 0x76))
    val PLAN_BADGE_FOREGROUND: Color = JBColor(Color(0x1A, 0x4E, 0xB0), Color(0xC3, 0xD5, 0xFF))
    val PLAN_ACTION_PRIMARY_FOREGROUND: Color = JBColor(Color.WHITE, Color.WHITE)
    val PLAN_ACTION_SECONDARY_FOREGROUND: Color = SELECTOR_TEXT_FOREGROUND

    // ── Error ─────────────────────────────────────────────────────────
    val ERROR_BACKGROUND: Color = JBColor(Color(0xFDECEA), Color(0x4A3434))
    val ERROR_FOREGROUND: Color = JBColor(Color(0x9F1D1D), Color(0xFFB4B4))

    // ── Borders / lines ───────────────────────────────────────────────
    val ROUNDED_BORDER_COLOR: Color = JBColor(Color(200, 200, 200), Color(70, 70, 70))
    val SPLIT_LINE_COLOR: Color = JBColor(Color(230, 238, 240), Color(30, 31, 34))
    val COMPOSER_BORDER_COLOR: Color = JBColor(Color(200, 210, 220), Color(65, 68, 72))
    val PLAN_CARD_BORDER_COLOR: Color = JBColor(Color(185, 202, 232), Color(78, 86, 98))
    val PLAN_BADGE_BACKGROUND: Color = JBColor(Color(246, 249, 255), Color(61, 67, 77))
    val PLAN_BADGE_BORDER: Color = JBColor(Color(190, 205, 233), Color(88, 98, 112))
    val PLAN_ACTION_PRIMARY_BACKGROUND: Color = JBColor(Color(0x1F, 0x5D, 0xE6), Color(0x4F, 0x85, 0xF5))
    val PLAN_ACTION_PRIMARY_HOVER_BACKGROUND: Color = JBColor(Color(0x1A, 0x52, 0xCB), Color(0x5F, 0x93, 0xFF))
    val PLAN_ACTION_PRIMARY_BORDER: Color = JBColor(Color(0x1A, 0x52, 0xCB), Color(0x6D, 0x9D, 0xFF))
    val PLAN_ACTION_PRIMARY_HOVER_BORDER: Color = JBColor(Color(0x14, 0x46, 0xAF), Color(0x7B, 0xA7, 0xFF))
    val PLAN_ACTION_SECONDARY_BACKGROUND: Color = JBColor(Color(255, 255, 255), Color(60, 64, 71))
    val PLAN_ACTION_SECONDARY_HOVER_BACKGROUND: Color = JBColor(Color(245, 248, 253), Color(67, 72, 80))
    val PLAN_ACTION_SECONDARY_BORDER: Color = JBColor(Color(202, 212, 228), Color(85, 92, 103))
    val PLAN_ACTION_SECONDARY_HOVER_BORDER: Color = JBColor(Color(180, 193, 214), Color(97, 106, 118))
    val ASK_BADGE_APPROVAL_BACKGROUND: Color = JBColor(Color(255, 247, 232), Color(77, 64, 42))
    val ASK_BADGE_APPROVAL_FOREGROUND: Color = JBColor(Color(0x9A, 0x5A, 0x00), Color(0xFF, 0xD6, 0x96))
    val ASK_BADGE_APPROVAL_BORDER: Color = JBColor(Color(238, 204, 147), Color(110, 91, 61))
    val ASK_BADGE_QUESTION_BACKGROUND: Color = JBColor(Color(239, 246, 255), Color(54, 63, 82))
    val ASK_BADGE_QUESTION_FOREGROUND: Color = JBColor(Color(0x1A, 0x4E, 0xB0), Color(0xC6, 0xD9, 0xFF))
    val ASK_BADGE_QUESTION_BORDER: Color = JBColor(Color(187, 205, 240), Color(84, 96, 122))
    val ASK_BADGE_REQUEST_BACKGROUND: Color = JBColor(Color(235, 250, 246), Color(46, 72, 66))
    val ASK_BADGE_REQUEST_FOREGROUND: Color = JBColor(Color(0x0B, 0x6A, 0x57), Color(0xA2, 0xF2, 0xDF))
    val ASK_BADGE_REQUEST_BORDER: Color = JBColor(Color(166, 219, 206), Color(72, 107, 98))
    val APPROVE_ACTION_BACKGROUND: Color = JBColor(Color(0x1F, 0x8A, 0x4C), Color(0x2D, 0xA0, 0x5E))
    val APPROVE_ACTION_HOVER_BACKGROUND: Color = JBColor(Color(0x19, 0x79, 0x42), Color(0x39, 0xB1, 0x6A))
    val APPROVE_ACTION_BORDER: Color = JBColor(Color(0x1A, 0x73, 0x40), Color(0x45, 0xBA, 0x72))
    val APPROVE_ACTION_HOVER_BORDER: Color = JBColor(Color(0x16, 0x63, 0x37), Color(0x55, 0xC6, 0x7D))
    val REJECT_ACTION_BACKGROUND: Color = JBColor(Color(0xF9, 0xEB, 0xEC), Color(0x6D, 0x47, 0x4B))
    val REJECT_ACTION_HOVER_BACKGROUND: Color = JBColor(Color(0xF5, 0xDD, 0xE0), Color(0x7C, 0x51, 0x56))
    val REJECT_ACTION_BORDER: Color = JBColor(Color(0xE0, 0xB7, 0xBC), Color(0x96, 0x68, 0x6E))
    val REJECT_ACTION_HOVER_BORDER: Color = JBColor(Color(0xD1, 0x9B, 0xA2), Color(0xA9, 0x77, 0x7E))
    val REJECT_ACTION_FOREGROUND: Color = JBColor(Color(0x8A, 0x25, 0x31), Color(0xFF, 0xD0, 0xD6))
    val INTERACTION_INPUT_BACKGROUND: Color = PLAN_CARD_SURFACE_BACKGROUND
    val INTERACTION_INPUT_BORDER: Color = PLAN_CARD_BORDER_COLOR

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
