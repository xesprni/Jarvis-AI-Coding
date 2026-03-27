package com.qihoo.finance.lowcode.aiquestion.ui.search.renderer

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel
import org.jetbrains.annotations.Contract
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*

/**
 * SearchPresentationFactory
 *
 * @author fengjinfu-jk
 * date 2024/8/12
 * @version 1.0.0
 * @apiNote SearchPresentationFactory
 */
class SearchPresentationFactory(private val editor: EditorImpl) {
    companion object {
        private val TEXT_METRICS_STORAGE = Key.create<SearchInlayTextMetricsStorage>("InlayTextMetricsStorage")

        private fun getTextMetricStorage(editor: EditorImpl): SearchInlayTextMetricsStorage {
            val storage = editor.getUserData(TEXT_METRICS_STORAGE)
            if (storage == null) {
                val newStorage = SearchInlayTextMetricsStorage(editor)
                editor.putUserData(TEXT_METRICS_STORAGE, newStorage)
                return newStorage
            }
            return storage
        }
    }

    @Contract(pure = true)
    fun smallText(text: String): InlayPresentation {
        val textWithoutBox =
            InsetPresentation(SearchTextInlayPresentation(textMetricsStorage, true, text), top = 1, down = 1)
        return withInlayAttributes(textWithoutBox)
    }

    @Contract(pure = true)
    fun onClick(
        base: InlayPresentation,
        buttons: EnumSet<MouseButton>,
        onClick: (MouseEvent, Point) -> Unit
    ): InlayPresentation {
        return OnClickPresentation(base) { e, p ->
            if (e.mouseButton in buttons) {
                onClick(e, p)
            }
        }
    }

    private fun attributes(
        base: InlayPresentation,
        textAttributesKey: TextAttributesKey,
        flags: WithAttributesPresentation.AttributesFlags = WithAttributesPresentation.AttributesFlags()
    ): WithAttributesPresentation =
        WithAttributesPresentation(base, textAttributesKey, editor, flags)

    fun withReferenceAttributes(noHighlightReference: InlayPresentation): WithAttributesPresentation {
        return attributes(
            noHighlightReference, EditorColors.REFERENCE_HYPERLINK_COLOR,
            WithAttributesPresentation.AttributesFlags().withSkipEffects(true)
        )
    }

    @Contract(pure = true)
    fun referenceOnHover(
        base: InlayPresentation,
        clickListener: InlayPresentationFactory.ClickListener
    ): InlayPresentation {
        val delegate = DynamicDelegatePresentation(base)
        val hovered = onClick(
            base = withReferenceAttributes(base),
            buttons = EnumSet.of(MouseButton.Left, MouseButton.Middle),
            onClick = { e, p ->
                clickListener.onClick(e, p)
            }
        )
        return OnHoverPresentation(delegate, object : InlayPresentationFactory.HoverListener {
            override fun onHover(event: MouseEvent, translated: Point) {
                val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                editor.setCustomCursor(this@SearchPresentationFactory, handCursor)
                delegate.delegate = hovered
            }

            override fun onHoverFinished() {
                delegate.delegate = base
                editor.setCustomCursor(this@SearchPresentationFactory, null)
            }
        })
    }

    @Contract(pure = true)
    fun roundWithBackground(base: InlayPresentation): InlayPresentation {
        val rounding = withInlayAttributes(
            RoundWithBackgroundPresentation(
                InsetPresentation(
                    base,
                    left = 7,
                    right = 7,
                    top = 0,
                    down = 0
                ),
                8,
                8,
                JBColor.BLUE,
            )
        )
        return SearchDynamicInsetPresentation(rounding, offsetFromTopProvider)
    }

    private fun withInlayAttributes(base: InlayPresentation): InlayPresentation {
        val textAttributesKey = DefaultLanguageHighlighterColors.INLAY_DEFAULT;
        textAttributesKey.defaultAttributes.setAttributes(
            JBColor.WHITE,
            JBColor.WHITE,
            JBColor.WHITE,
            JBColor.RED,
            EffectType.ROUNDED_BOX,
            0
        )
        return WithAttributesPresentation(
            base, textAttributesKey, editor,
            WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        )
    }

    private val textMetricsStorage = getTextMetricStorage(editor)
    private val offsetFromTopProvider = object : SearchInsetValueProvider {
        override val top: Int
            get() = textMetricsStorage.getFontMetrics(true).offsetFromTop()
    }
}
