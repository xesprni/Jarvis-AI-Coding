package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

public interface ChatxEditorSupport {

    public static final ExtensionPointName<ChatxEditorSupport> EP = ExtensionPointName.create("ai.chatx.plugin.editorSupport");

    static boolean isEditorCompletionsSupported(@NotNull Editor editor) {
        if (!EP.hasAnyExtensions()) {
            return true;
        }
        return (EP.findFirstSafe(editorSupport -> !editorSupport.isCompletionsEnabled(editor)) == null);
    }

    boolean isCompletionsEnabled(@NotNull Editor paramEditor);
}
