package com.qifu.ui.smartconversation.textarea

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ComponentUtil.findParentByCondition
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*

class PromptTextFieldEventDispatcher(
    private val dispatcherId: UUID,
    private val onBackSpace: () -> Unit,
    private val onSubmit: (KeyEvent) -> Unit
) : IdeEventQueue.EventDispatcher {

    override fun dispatch(e: AWTEvent): Boolean {
        if ((e is KeyEvent || e is MouseEvent) && findParent() is PromptTextField) {
            if (e is KeyEvent) {
                // 优先检查是否需要拦截 lookup 的默认行为
                if (e.id == KeyEvent.KEY_TYPED) {
                    if (handleLookupInterception(e)) {
                        return true
                    }
                } else if (e.id == KeyEvent.KEY_PRESSED) {
                    when (e.keyCode) {
                        KeyEvent.VK_BACK_SPACE -> {
                            if (!handleBackspace(e)) {
                                onBackSpace()
                            }
                        }

                        KeyEvent.VK_DELETE -> handleDelete(e)
                        KeyEvent.VK_A -> if (e.isControlDown || e.isMetaDown) handleSelectAll(e)
                        KeyEvent.VK_V -> {
                            if (e.isControlDown || e.isMetaDown) {
                                if (handlePaste(e)) {
                                    return true
                                }
                            }
                        }
                        KeyEvent.VK_LEFT -> handleCursorMove(e, isLeft = true)
                        KeyEvent.VK_RIGHT -> handleCursorMove(e, isLeft = false)
                        KeyEvent.VK_ENTER -> {
                            // No modifiers configured: send on plain Enter
                            if (!e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                                onSubmit(e)
                            } else {
                                handleNewline(e)
                            }
                        }
                        KeyEvent.VK_PERIOD -> {
                            val parent = findParent() as PromptTextField
                            val lookup = parent.lookup
                            if (lookup != null && lookup.isShown && !lookup.isLookupDisposed) {
                                e.consume()
                                return true
                            }
                        }
                    }
                }

                return e.isConsumed
            }
        }
        return false
    }

    private fun findParent(): Component? {
        return findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner) { component ->
            component is PromptTextField && component.dispatcherId == dispatcherId
        }
    }

    private fun handleNewline(e: KeyEvent) {
        val parent = findParent()
        if (parent is PromptTextField) {
            runUndoTransparentWriteAction {
                val document = parent.document
                val caretModel = parent.editor?.caretModel
                val offset = caretModel?.offset ?: return@runUndoTransparentWriteAction

                // 简单地插入换行符，不删除和重新插入文本
                // 这样可以保持 RangeHighlighter 的有效性
                document.insertString(offset, "\n")
                caretModel.moveToOffset(offset + 1)
            }
            e.consume()
        }
    }

    private fun handleSelectAll(e: KeyEvent) {
        val parent = findParent()
        if (parent is PromptTextField) {
            parent.editor?.let { editor ->
                editor.selectionModel.setSelection(0, editor.document.textLength)
                e.consume()
            }
        }
    }

    private fun handlePaste(e: KeyEvent): Boolean {
        val parent = findParent()
        if (parent is PromptTextField) {
            val clipText: String? = try { CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor) as? String } catch (_: Exception) { null }
            if (clipText.isNullOrEmpty()) return false
            parent.insertPlaceholderFor(clipText)
            e.consume()
            return true
        }
        return false
    }

    private fun handleDelete(e: KeyEvent) {
        val parent = findParent()
        if (parent is PromptTextField) {
            parent.editor?.let { editor ->
                if (parent.handlePlaceholderDelete(isBackspace = false)) {
                    e.consume()
                    return
                }
                if (parent.handleSlashTokenDelete(isBackspace = false)) {
                    e.consume()
                    return
                }
                runUndoTransparentWriteAction {
                    val document = editor.document
                    val caretModel = editor.caretModel
                    val selectionModel = editor.selectionModel

                    if (selectionModel.hasSelection()) {
                        document.deleteString(
                            selectionModel.selectionStart,
                            selectionModel.selectionEnd
                        )
                    } else {
                        val offset = caretModel.offset
                        if (offset < document.textLength) {
                            document.deleteString(offset, offset + 1)
                        }
                    }
                }
                e.consume()
            }
        }
    }

    private fun handleBackspace(e: KeyEvent): Boolean {
        val parent = findParent()
        if (parent is PromptTextField) {
            parent.editor?.let { editor ->
                if (parent.handlePlaceholderDelete(isBackspace = true)) {
                    e.consume()
                    return true
                }
                if (parent.handleSlashTokenDelete(isBackspace = true)) {
                    e.consume()
                    return true
                }
                val selectionModel = editor.selectionModel
                if (selectionModel.hasSelection()) {
                    runUndoTransparentWriteAction {
                        editor.document.deleteString(
                            selectionModel.selectionStart,
                            selectionModel.selectionEnd
                        )
                    }
                    e.consume()
                    return true
                } else if (e.isControlDown || e.isMetaDown) {
                    runUndoTransparentWriteAction {
                        val document = editor.document
                        val caretModel = editor.caretModel
                        val offset = caretModel.offset
                        if (offset > 0) {
                            val text = document.text
                            var wordStart = offset - 1

                            while (wordStart > 0 && Character.isWhitespace(text[wordStart])) {
                                wordStart--
                            }

                            while (wordStart > 0 && !Character.isWhitespace(text[wordStart - 1])) {
                                wordStart--
                            }

                            document.deleteString(wordStart, offset)
                        }
                    }
                    e.consume()
                    return true
                }
            }
        }
        return false
    }

    private fun handleCursorMove(e: KeyEvent, isLeft: Boolean) {
        val parent = findParent()
        if (parent is PromptTextField) {
            parent.editor?.let { editor ->
                val caretModel = editor.caretModel
                val currentOffset = caretModel.offset

                // 检查下一个位置是否是placeholder的边界 [start, end)
                val nextOffset = if (isLeft) currentOffset - 1 else currentOffset + 1
                if (nextOffset >= 0 && nextOffset <= editor.document.textLength) {
                    val nextPlaceholder = parent.findPlaceholderAtOffset(nextOffset)
                    if (nextPlaceholder != null) {
                        val start = nextPlaceholder.highlighter.startOffset
                        val end = nextPlaceholder.highlighter.endOffset
                        
                        if (isLeft) {
                            caretModel.moveToOffset(start)
                            e.consume()
                        } else {
                            if (nextOffset != start) {
                                caretModel.moveToOffset(end)
                                e.consume()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理 lookup 弹窗显示时的 . 和空格键拦截
     * @return true 表示事件已被拦截并消费
     */
    private fun handleLookupInterception(e: KeyEvent): Boolean {
        val parent = findParent()
        if (parent is PromptTextField) {
            val lookup = parent.lookup
            if (lookup != null && lookup.isShown && !lookup.isLookupDisposed) {
                val char = e.keyChar
                if (char == '.' || char == ' ') {
                    parent.editor?.let { editor ->
                        runUndoTransparentWriteAction {
                            val caretModel = editor.caretModel
                            val offset = editor.caretModel.offset
                            editor.document.insertString(offset, char.toString())
                            caretModel.moveToOffset(offset + 1)
                        }
                    }
                    e.consume()
                    return true
                }
            }
        }
        return false
    }
}
