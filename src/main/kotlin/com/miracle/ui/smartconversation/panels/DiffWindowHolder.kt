package com.miracle.ui.smartconversation.panels

import com.intellij.openapi.project.Project

object DiffWindowHolder {
    fun showDiffView(
        project: Project,
        title: String,
        oldContent: String,
        newContent: String,
        convId: String?,
        filePath: String?,
        focus: Boolean,
    ) {
        // The simplified Kotlin-only plugin no longer opens a dedicated diff UI here.
    }
}
