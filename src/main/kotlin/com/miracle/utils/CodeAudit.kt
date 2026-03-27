package com.miracle.utils

import com.intellij.openapi.project.Project

object CodeAudit {
    fun recordPreview(
        project: Project,
        requestId: String,
        filePath: String,
        content: String,
    ) {
        // Intentionally local-only. Historical remote completion audit was removed.
    }

    fun markAccepted(requestId: String) {
        // Intentionally local-only.
    }

    fun markRejected(requestId: String) {
        // Intentionally local-only.
    }
}
