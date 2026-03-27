package com.miracle.ui.core

sealed interface ChatComposerInsertion {
    data class PlainText(val text: String) : ChatComposerInsertion

    data class CodeReference(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val code: String,
    ) : ChatComposerInsertion

    data class PathReference(
        val path: String,
        val directory: Boolean = false,
    ) : ChatComposerInsertion
}
