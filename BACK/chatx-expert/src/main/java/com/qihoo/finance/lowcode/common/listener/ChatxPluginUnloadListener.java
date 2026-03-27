package com.qihoo.finance.lowcode.common.listener;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.common.util.ChatxPlugin;
import com.qihoo.finance.lowcode.editor.ChatxEditorManager;
import com.qihoo.finance.lowcode.editor.InlayDisposeContext;
import org.jetbrains.annotations.NotNull;

public class ChatxPluginUnloadListener implements DynamicPluginListener {

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (ChatxPlugin.isChatxPlugin(pluginDescriptor)) {
            disposeEditorInlays();
        }
    }

    private static void disposeEditorInlays() {
        for (Project project : ApplicationUtil.findValidProjects()) {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null && !editor.isDisposed())
                ChatxEditorManager.getInstance().disposeInlays(editor, InlayDisposeContext.UserAction);
        }
    }
}
