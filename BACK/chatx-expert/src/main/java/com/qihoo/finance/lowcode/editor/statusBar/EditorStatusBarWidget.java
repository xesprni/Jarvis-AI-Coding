package com.qihoo.finance.lowcode.editor.statusBar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorStatusBarWidget extends EditorBasedStatusBarPopup {

    private static final String WIDGET_ID = "chatx.editorStatusBarWidget";
    static String statusBarText;

    public EditorStatusBarWidget(@NotNull Project project) {
        super(project, false);
    }

    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        Pair<ChatxStatus, String> statusAndMessage = ChatxStatusService.getCurrentStatus();
        ChatxStatus status = statusAndMessage.first;
        if (status.isIconAlwaysShown()) {
            String message = statusAndMessage.second;
            String str1 = message == null ? ChatxBundle.get("statusBar.tooltipForError", status.getPresentableText())
                    : ChatxBundle.get("statusBar.tooltipForErrorCustomMessage", status.getPresentableText(), message);
            EditorBasedStatusBarPopup.WidgetState widgetState = new EditorBasedStatusBarPopup.WidgetState(str1, statusBarText, true);
            widgetState.setIcon(status.getIcon());
            return widgetState;
        }
        if (file == null) {
            return EditorBasedStatusBarPopup.WidgetState.HIDDEN;
        }
        Boolean enabled = isChatxEnabled(file);
        if (enabled == null) {
            return EditorBasedStatusBarPopup.WidgetState.HIDDEN;
        }
        String text = getStatusBarText(enabled, status);
        String toolTip = ChatxApplicationSettings.settings().pluginName;
        EditorBasedStatusBarPopup.WidgetState state = new EditorBasedStatusBarPopup.WidgetState(toolTip, text, true);
        state.setIcon(enabled ? status.getIcon() : Icons.STATUS_BAR_DISABLED);
        return state;
    }

    private String getStatusBarText(boolean enabled, ChatxStatus status) {
        if (!enabled) {
            return " " + ChatxBundle.get("chatx.completion.statusBar.disabled.text");
        }
        if (status == ChatxStatus.NotSignedIn) {
            return " " +  ChatxBundle.get("chatx.completion.statusBar.notSignedIn.text");
        }
        // 补全可用时，才取statusBarText作为状态栏的文本
        if (statusBarText == null) {
            statusBarText = " " + ChatxBundle.get("chatx.completion.statusBar.enabled.text");
        }
        return statusBarText;
    }

    @Nullable
    private Boolean isChatxEnabled(@NotNull VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(this.myProject).findFile(file);
        if (psiFile == null)
            return null;
        return ChatxApplicationSettings.isChatxEnabled(psiFile.getLanguage());
    }

    @Override
    protected @Nullable ListPopup createPopup(DataContext context) {
        return createPopup(context, false);
    }

    @Nullable
    private ListPopup createPopup(DataContext context, boolean withStatusItem) {
        ActionGroup group;
        ChatxStatus currentStatus = (ChatxStatusService.getCurrentStatus()).first;
        if (currentStatus == ChatxStatus.Unsupported)
            return null;
        AnAction configuredGroup = ActionManager.getInstance().getAction(findPopupMenuId(currentStatus));
        if (!(configuredGroup instanceof ActionGroup))
            return null;
        if (withStatusItem) {
            DefaultActionGroup statusGroup = new DefaultActionGroup();
            statusGroup.add(new StatusItemAction());
            statusGroup.addSeparator();
            statusGroup.addAll(new AnAction[] { configuredGroup });
            group = statusGroup;
        } else {
            group = (ActionGroup)configuredGroup;
        }
        return JBPopupFactory.getInstance().createActionGroupPopup(
                ChatxBundle.get("statusBar.popup.title"), group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH
                , withStatusItem);
    }

    @NotNull
    private String findPopupMenuId(@NotNull ChatxStatus currentStatus) {
        if (currentStatus == ChatxStatus.NotSignedIn)
            return "Chatx.notSignedInStatusBarPopup";
        if (currentStatus == ChatxStatus.Ready)
            return "Chatx.statusBarPopup";
        if (currentStatus.isDisablingClientRequests())
            return "Chatx.statusBarRestartPopup";
        return "Chatx.statusBarErrorPopup";
    }

    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new EditorStatusBarWidget(project);
    }

    @Override
    public @NonNls @NotNull String ID() {
        return WIDGET_ID;
    }

    public static void update(@NotNull Project project) {
        EditorStatusBarWidget widget = findWidget(project);
        if (widget != null)
            widget.update(() -> widget.myStatusBar.updateWidget(WIDGET_ID));
    }

    public static void update(@NotNull Project project, String statusBarText) {
        EditorStatusBarWidget.statusBarText = statusBarText;
        EditorStatusBarWidget widget = findWidget(project);
        if (widget != null)
            widget.update(() -> widget.myStatusBar.updateWidget(WIDGET_ID));
    }

    @Nullable
    private static EditorStatusBarWidget findWidget(@NotNull Project project) {
        StatusBar bar = WindowManager.getInstance().getStatusBar(project);
        if (bar != null) {
            StatusBarWidget widget = bar.getWidget(WIDGET_ID);
            if (widget instanceof EditorStatusBarWidget)
                return (EditorStatusBarWidget)widget;
        }
        return null;
    }
}
