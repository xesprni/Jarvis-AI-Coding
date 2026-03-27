package com.qihoo.finance.lowcode.settings;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "chatx-settings", storages = {@Storage("chatx-settings.xml")})
public class ChatxApplicationSettings implements PersistentStateComponent<ChatxApplicationState> {

    private ChatxApplicationState state;

    public static ChatxApplicationState settings() {
        return ApplicationManager.getApplication().getService(ChatxApplicationSettings.class).getState();
    }

    public static boolean isChatxEnabled(@NotNull Project project, @NotNull Editor editor) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        return (file != null && isChatxEnabled(file));
    }

    public static boolean isChatxEnabled(@NotNull PsiFile file) {
        Language language = file.getLanguage();
        return isChatxEnabled(language);
    }

    public static boolean isChatxEnabled(@NotNull Language language) {
        ChatxApplicationState settings = settings();
        return (settings.enableCompletions && settings.isEnabled(language));
    }

    @Override
    public @Nullable ChatxApplicationState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull ChatxApplicationState state) {
        this.state = state;
    }

    @Override
    public void noStateLoaded() {
        this.state = new ChatxApplicationState();
    }
}
