package com.qifu.ui.smartconversation.editor

import com.intellij.openapi.vfs.VirtualFile

data class ToolWindowEditorFileDetails(val path: String, val virtualFile: VirtualFile? = null)