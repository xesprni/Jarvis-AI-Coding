package com.miracle.utils

import com.intellij.openapi.project.Project

/**
 * 代码审计工具，用于记录补全预览和接受/拒绝情况
 */
object CodeAudit {
    /**
     * 记录补全预览
     * @param project 当前项目
     * @param requestId 请求 ID
     * @param filePath 文件路径
     * @param content 补全内容
     */
    fun recordPreview(
        project: Project,
        requestId: String,
        filePath: String,
        content: String,
    ) {
        // Intentionally local-only. Historical remote completion audit was removed.
    }

    /**
     * 标记补全建议已被接受
     * @param requestId 请求 ID
     */
    fun markAccepted(requestId: String) {
        // Intentionally local-only.
    }

    /**
     * 标记补全建议已被拒绝
     * @param requestId 请求 ID
     */
    fun markRejected(requestId: String) {
        // Intentionally local-only.
    }
}
