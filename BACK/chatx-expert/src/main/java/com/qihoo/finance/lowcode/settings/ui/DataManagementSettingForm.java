package com.qihoo.finance.lowcode.settings.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.TitledSeparator;
import com.qifu.utils.ConversationStore;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static com.qifu.utils.ConfigKt.getUserConfigDirectory;

/**
 * 数据管理设置页面
 */
public class DataManagementSettingForm implements Configurable {

    public static final String TITLE = "数据管理";

    private JButton deleteAllConversationsButton;
    private JButton manageStorageButton;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable JComponent createComponent() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 添加标题分隔符
        TitledSeparator separator = new TitledSeparator("会话管理");
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(separator);
        
        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 创建删除按钮行
        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        deletePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel deleteLabel = new JLabel("删除所有会话：");
        deleteAllConversationsButton = new JButton("删除");

        // 添加删除确认逻辑
        deleteAllConversationsButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                mainPanel,
                "确定要删除所有会话吗？此操作不可撤销！",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                deleteAllConversations();
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "所有会话已成功删除",
                    "删除成功",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        
        deletePanel.add(deleteLabel);
        deletePanel.add(deleteAllConversationsButton);
        
        // 添加说明文字
        JLabel descLabel = new JLabel("（将删除当前项目的会话历史记录）");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, descLabel.getFont().getSize() - 1));
        descLabel.setForeground(Color.GRAY);
        deletePanel.add(descLabel);
        
        contentPanel.add(deletePanel);
        
        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 创建存储空间管理行
        JPanel storagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        storagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel storageLabel = new JLabel("存储空间：");
        manageStorageButton = new JButton("管理");
        
        // 添加管理按钮逻辑
        manageStorageButton.addActionListener(e -> {
            openStorageDirectory();
        });
        
        storagePanel.add(storageLabel);
        storagePanel.add(manageStorageButton);
        
        // 添加说明文字
        JLabel storageDescLabel = new JLabel("（打开 Jarvis 配置目录）");
        storageDescLabel.setFont(storageDescLabel.getFont().deriveFont(Font.PLAIN, storageDescLabel.getFont().getSize() - 1));
        storageDescLabel.setForeground(Color.GRAY);
        storagePanel.add(storageDescLabel);
        
        contentPanel.add(storagePanel);
        
        // 添加顶部和左侧边距
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapperPanel.add(contentPanel, BorderLayout.NORTH);
        
        mainPanel.add(wrapperPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }

    /**
     * 删除所有会话
     */
    private void deleteAllConversations() {
        // 获取当前项目
        Project currentProject = getCurrentProject();
        if (currentProject == null) {
            JOptionPane.showMessageDialog(
                null,
                "无法获取当前项目",
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // 删除当前项目的所有会话
        var conversations = ConversationStore.getConversations(currentProject);
        for (var conversation : conversations) {
            ConversationStore.deleteConversation(currentProject, conversation.getId());
        }
    }
    
    /**
     * 获取当前项目
     */
    private Project getCurrentProject() {
        // 从当前打开的窗口获取项目
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return null;
        }
        
        var lastFocusedFrame = com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance()
            .getLastFocusedFrame();
        
        return lastFocusedFrame != null ? lastFocusedFrame.getProject() : null;
    }

    /**
     * 打开存储目录
     */
    private void openStorageDirectory() {
        try {
            String configDir = getUserConfigDirectory();
            File configFile = new File(configDir);
            
            if (!configFile.exists()) {
                configFile.mkdirs();
            }
            
            // 使用系统默认文件管理器打开目录
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(configFile);
            } else {
                // 如果 Desktop 不支持，尝试使用 BrowserUtil
                BrowserUtil.browse(configFile.toURI());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                null,
                "无法打开目录: " + ex.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    public boolean isModified() {
        // 数据管理页面没有需要保存的配置项
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        // 无需保存配置
    }
}
