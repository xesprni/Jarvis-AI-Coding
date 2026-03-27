package com.miracle.ui.core

import java.io.File

sealed interface AssociatedContextItem {
    val key: String

    data class AssociatedFile(
        val path: String,
    ) : AssociatedContextItem {
        override val key: String = "file:$path"

        val displayName: String
            get() = File(path).name.ifBlank { path }
    }

    data class AssociatedCodeSelection(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val fullLineText: String,
    ) : AssociatedContextItem {
        override val key: String = "code:$filePath:$startLine:$endLine:${fullLineText.hashCode()}"

        val displayName: String
            get() = "${File(filePath).name} ($startLine-$endLine)"
    }
}
