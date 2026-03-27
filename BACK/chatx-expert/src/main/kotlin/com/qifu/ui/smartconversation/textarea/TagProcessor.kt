package com.qifu.ui.smartconversation.textarea

import com.qihoo.finance.lowcode.smartconversation.conversations.Message


fun interface TagProcessor {
    fun process(message: Message, promptBuilder: StringBuilder)
}
