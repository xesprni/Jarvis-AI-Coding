package com.qifu.ui.smartconversation.psistructure

import com.qifu.ui.smartconversation.textarea.header.TagDetails


sealed class PsiStructureState {

    data class UpdateInProgress(
        val currentlyAnalyzedTags: Set<TagDetails>,
    ) : PsiStructureState()

    data object Disabled : PsiStructureState()

    data class Content(
        val currentlyAnalyzedTags: Set<TagDetails>,
        val elements: Set<ClassStructure>
    ) : PsiStructureState()
}