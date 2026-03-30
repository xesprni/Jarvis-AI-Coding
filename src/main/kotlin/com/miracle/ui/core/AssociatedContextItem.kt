package com.miracle.ui.core

import java.io.File

/**
 * 聊天关联上下文项的密封接口，定义了统一的唯一键。
 */
sealed interface AssociatedContextItem {
    /** 条目的唯一标识键 */
    val key: String

    /**
     * 关联文件上下文项。
     *
     * @property path 文件路径
     */
    data class AssociatedFile(
        val path: String,
    ) : AssociatedContextItem {
        override val key: String = "file:$path"

        /** 文件显示名称 */
        val displayName: String
            get() = File(path).name.ifBlank { path }
    }

    /**
     * 关联代码选区上下文项。
     *
     * @property filePath 文件路径
     * @property startLine 起始行号
     * @property endLine 结束行号
     * @property fullLineText 完整行文本内容
     */
    data class AssociatedCodeSelection(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val fullLineText: String,
    ) : AssociatedContextItem {
        override val key: String = "code:$filePath:$startLine:$endLine:${fullLineText.hashCode()}"

        /** 代码选区显示名称 */
        val displayName: String
            get() = "${File(filePath).name} ($startLine-$endLine)"
    }
}
