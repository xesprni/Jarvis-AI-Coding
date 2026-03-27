package com.qihoo.finance.lowcode.smartconversation.configuration;

import com.intellij.openapi.util.Key;
import com.qifu.ui.smartconversation.textarea.PromptTextField;

import java.util.List;

/**
 * @author weiyichao
 * @date 2025-10-29
 **/
public class JarvisKeys {
    public static final Key<List<String>> IMAGE_ATTACHMENT_FILE_PATH = Key.create("jarvis.imageAttachmentFilePath");

    public static final Key<Boolean> IS_PROMPT_TEXT_FIELD_DOCUMENT = Key.create("jarvis.isPromptTextFieldDocument");

    public static final Key<PromptTextField>  PROMPT_FIELD_KEY = Key.create("jarvis.promptTextField.instance");
}
