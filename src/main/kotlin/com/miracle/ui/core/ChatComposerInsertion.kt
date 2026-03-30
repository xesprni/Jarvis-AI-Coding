package com.miracle.ui.core

/**
 * 聊天输入框插入内容的密封接口，定义不同类型的可插入内容。
 */
sealed interface ChatComposerInsertion {
    /** 纯文本插入 */
    data class PlainText(val text: String) : ChatComposerInsertion

    /**
     * 代码引用插入。
     *
     * @property filePath 文件路径
     * @property startLine 起始行号
     * @property endLine 结束行号
     * @property code 代码内容
     */
    data class CodeReference(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val code: String,
    ) : ChatComposerInsertion

    /**
     * 路径引用插入。
     *
     * @property path 路径
     * @property directory 是否为目录，默认 false
     */
    data class PathReference(
        val path: String,
        val directory: Boolean = false,
    ) : ChatComposerInsertion
}
