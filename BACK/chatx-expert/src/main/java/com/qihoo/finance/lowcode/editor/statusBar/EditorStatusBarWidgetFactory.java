package com.qihoo.finance.lowcode.editor.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class EditorStatusBarWidgetFactory extends StatusBarEditorBasedWidgetFactory {

    @Override
    public @NonNls @NotNull String getId() {
        return "ChatX.editorStatusBar";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return ChatxBundle.get("statusBar.displayName");
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new EditorStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {

    }
}
