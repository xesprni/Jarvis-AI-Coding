package com.qifu.ui.smartconversation.textarea.header

interface TagManagerListener {
    fun onTagAdded(tag: TagDetails)
    fun onTagRemoved(tag: TagDetails)
    fun onTagSelectionChanged(tag: TagDetails)
}