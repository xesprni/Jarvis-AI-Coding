package com.qifu.ui.smartconversation.textarea

import com.qifu.ui.smartconversation.textarea.header.CodeAnalyzeTagDetails
import com.qifu.ui.smartconversation.textarea.header.CurrentGitChangesTagDetails
import com.qifu.ui.smartconversation.textarea.header.DocumentationTagDetails
import com.qifu.ui.smartconversation.textarea.header.EditorSelectionTagDetails
import com.qifu.ui.smartconversation.textarea.header.EditorTagDetails
import com.qifu.ui.smartconversation.textarea.header.FileTagDetails
import com.qifu.ui.smartconversation.textarea.header.FolderTagDetails
import com.qifu.ui.smartconversation.textarea.header.GitCommitTagDetails
import com.qifu.ui.smartconversation.textarea.header.PersonaTagDetails
import com.qifu.ui.smartconversation.textarea.header.SelectionTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagDetails
import com.qifu.ui.smartconversation.textarea.header.WebTagDetails


internal class TagDetailsComparator : Comparator<TagDetails> {
    override fun compare(o1: TagDetails, o2: TagDetails): Int {
        return getPriority(o1).compareTo(getPriority(o2))
    }

    private fun getPriority(tag: TagDetails): Int {
        if (!tag.selected && tag !is CodeAnalyzeTagDetails) {
            return Int.MAX_VALUE
        }

        return when (tag) {
            is CodeAnalyzeTagDetails,
            is EditorSelectionTagDetails -> 0

            is SelectionTagDetails -> 5
            is DocumentationTagDetails,
            is PersonaTagDetails,
            is GitCommitTagDetails,
            is CurrentGitChangesTagDetails,
            is FolderTagDetails,
            is WebTagDetails -> 10

            is EditorTagDetails,
            is FileTagDetails -> 15

            else -> 20
        }
    }
}
