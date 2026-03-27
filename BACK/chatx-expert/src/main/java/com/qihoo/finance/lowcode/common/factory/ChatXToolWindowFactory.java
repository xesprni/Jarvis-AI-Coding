package com.qihoo.finance.lowcode.common.factory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.qihoo.finance.lowcode.apitrack.ui.ApiMainPanel;
import com.qihoo.finance.lowcode.common.action.ChatHistoryAction;
import com.qihoo.finance.lowcode.common.action.UserInfoAction;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.OAuth2LoginPanel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.design.ui.DatabaseMainPanel;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author weiyichao
 * @date 2023-07-26
 **/
@Slf4j
public class ChatXToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {
    public static Color BACKGROUND;

    public static final String TAB_AGENT_NAME = "智能会话";

    public static final String TAB_DATABASE_NAME = "数据库代码";

    public static final String TAB_API_NAME = "接口代码";


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        log.info("createToolWindowContent, toolWindow: {}", toolWindow.getId());
        if (isLoggedIn()){
            SmartToolWindowPanel smartToolWindowPanel = new SmartToolWindowPanel(project,toolWindow.getDisposable());
            addContent(toolWindow, smartToolWindowPanel, TAB_AGENT_NAME,Icons.scaleToWidth(Icons.ASK_AI_LIGHT, 16));
            
            addLazyContent(toolWindow, TAB_DATABASE_NAME, Icons.scaleToWidth(Icons.DB_GEN, 16),
                    () -> project.getService(DatabaseMainPanel.class).createPanel());
            
            addLazyContent(toolWindow, TAB_API_NAME, Icons.scaleToWidth(Icons.API, 16),
                    () -> project.getService(ApiMainPanel.class).createPanel());

        }
        else {
            JComponent loginPanel = project.getService(OAuth2LoginPanel.class).createPanel();
            addContent(toolWindow, loginPanel, "登录",Icons.scaleToWidth(Icons.ASK_AI_LIGHT, 16));
        }

        toolWindow.getContentManager().setSelectedContent(Objects.requireNonNull(toolWindow.getContentManager().getContent(0)));

        if (toolWindow instanceof ToolWindowEx toolWindowEx){
            //设置工具栏图标，新增设置按钮
            toolWindowEx.setTitleActions(new ChatHistoryAction(project),new UserInfoAction(project));
        }
    }


    public void addContent(ToolWindow toolWindow, JComponent content, String title,Icon icon) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content tabContent = contentFactory.createContent(content, title, false);
        tabContent.setPopupIcon(icon);
        tabContent.setCloseable(false);
        tabContent.setIcon(icon);
        toolWindow.getContentManager().addContent(tabContent);
    }
    
    private void addLazyContent(ToolWindow toolWindow, String title, Icon icon, java.util.function.Supplier<JComponent> contentSupplier) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        JPanel placeholder = new JPanel(new BorderLayout());
        Content tabContent = contentFactory.createContent(placeholder, title, false);
        tabContent.setPopupIcon(icon);
        tabContent.setCloseable(false);
        tabContent.setIcon(icon);
        
        toolWindow.getContentManager().addContent(tabContent);
        
        toolWindow.addContentManagerListener(new ContentManagerListener() {
            private boolean initialized = false;
            
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (title.equals(event.getContent().getDisplayName()) && event.getContent().isSelected() && !initialized) {
                    initialized = true;
                    JComponent actualContent = contentSupplier.get();
                    placeholder.removeAll();
                    placeholder.add(actualContent, BorderLayout.CENTER);
                    placeholder.revalidate();
                    placeholder.repaint();
                }
            }
        });
    }

    public boolean isLoggedIn(){
        UserInfoPersistentState userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        UserInfoPersistentState.UserInfo userInfo = userInfoPersistentState.getState();
        return userInfo != null && StringUtils.isNotEmpty(userInfo.email) && StringUtils.isNotEmpty(userInfo.token);
    }

    public static ToolWindow getToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(ProjectUtils.getCurrProject());
        return toolWindowManager.getToolWindow(Constants.PLUGIN_TOOL_WINDOW_ID);
    }

    public static Content getSelectedContent() {
        ToolWindow toolWindow = ChatXToolWindowFactory.getToolWindow();
        return toolWindow.getContentManager().getSelectedContent();
    }

    public static boolean isHidden() {
        return !ChatXToolWindowFactory.getToolWindow().isVisible();
    }

    public static ToolWindow show() {
        ToolWindow toolWindow = ChatXToolWindowFactory.getToolWindow();
        toolWindow.setAvailable(true, null);
        toolWindow.show(null);
        return toolWindow;
    }

    public static void showFirstTab() {
        ToolWindow toolWindow = ChatXToolWindowFactory.getToolWindow();
        if (!toolWindow.isVisible()) {
            toolWindow.setAvailable(true, null);
            toolWindow.show(null);
        }
        toolWindow.getContentManager().setSelectedContent(Objects.requireNonNull(toolWindow.getContentManager().getContent(0)));
    }

    public static void removeContent(Content content, boolean dispose) {
        ToolWindow toolWindow = ChatXToolWindowFactory.getToolWindow();
        toolWindow.getContentManager().removeContent(content, dispose);
    }

    @SuppressWarnings("all")
    @Override
    public @Nullable Icon getIcon() {
        return Icons.scaleToWidth(Icons.LOGO_ROUND, 16);
    }
}
