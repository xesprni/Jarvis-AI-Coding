package com.qihoo.finance.lowcode.settings.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import java.util.List;
import com.qihoo.finance.lowcode.aiquestion.ui.layout.MyBoxLayout;
import com.qifu.config.AutoApproveSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AutoApprovalSettingForm implements Configurable {

    public static final String TITLE = "自动批准设置";

    // 操作部分的组件
    private JBCheckBox readProjectFiles;
    private JLabel readProjectFilesDesc;
    private JBCheckBox readAllFiles;
    private JLabel readAllFilesDesc;
    private JBCheckBox editProjectFiles;
    private JLabel editProjectFilesDesc;

    private JBCheckBox executeSafeCommands;
    private JLabel executeSafeCommandsDesc;
    private JBCheckBox executeAllCommands;
    private JLabel executeAllCommandsDesc;
    private JBCheckBox useMCPServer;
    private JLabel useMCPServerDesc;

    // 最大请求数
    private JBTextField maxRequests;
    private JLabel maxRequestsDesc;

    // 黑名单
    private TagInputField blacklistCommands;
    private JLabel blacklistDesc;
    private JButton blackAddButton;
    private JTextField blackText;

    // Task任务执行
    private JBCheckBox runTask;
    private JLabel runTaskDesc;

    // Skill执行
    private JBCheckBox runSkill;
    private JLabel runSkillDesc;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable JComponent createComponent() {
        // 初始化组件
        initializeComponents();

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        MyBoxLayout boxLayout = new MyBoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        // 添加操作部分
        operationSetting(mainPanel);

        return mainPanel;
    }

    private void initializeComponents() {
        AutoApproveSettings.State state = AutoApproveSettings.Companion.getState();
        AutoApproveSettings.State.Actions actions = state.getActions();
        // 操作部分

        readProjectFiles = new JBCheckBox("读取项目文件");
        readProjectFiles.setSelected(actions.getReadFiles());
        readProjectFiles.setEnabled(false);
        readProjectFilesDesc = new JLabel("（允许Jarvis读取工作区内的文件）");
        readProjectFilesDesc.setEnabled(false);
        readProjectFilesDesc.setFont(readProjectFilesDesc.getFont().deriveFont(Font.PLAIN, readProjectFilesDesc.getFont().getSize() - 1));
        readProjectFilesDesc.setForeground(Color.GRAY);

        readAllFiles = new JBCheckBox("读取所有文件");
        readAllFiles.setSelected(actions.getReadFilesExternally());
        readAllFilesDesc = new JLabel("（允许Jarvis读取计算机上的任何文件）");
        readAllFilesDesc.setFont(readAllFilesDesc.getFont().deriveFont(Font.PLAIN, readAllFilesDesc.getFont().getSize() - 1));
        readAllFilesDesc.setForeground(Color.GRAY);

        editProjectFiles = new JBCheckBox("编辑项目文件");
        editProjectFiles.setSelected(actions.getEditFiles());
        editProjectFilesDesc = new JLabel("（允许Jarvis修改工作区内的文件）");
        editProjectFilesDesc.setFont(editProjectFilesDesc.getFont().deriveFont(Font.PLAIN, editProjectFilesDesc.getFont().getSize() - 1));
        editProjectFilesDesc.setForeground(Color.GRAY);

        executeSafeCommands = new JBCheckBox("执行终端安全命令");
        executeSafeCommands.setSelected(actions.getExecuteSafeCommands());
        executeSafeCommandsDesc = new JLabel("（允许Jarvis执行安全的终端命令。如果模型判断命令具有破坏性，仍需要批准）");
        executeSafeCommandsDesc.setFont(executeSafeCommandsDesc.getFont().deriveFont(Font.PLAIN, executeSafeCommandsDesc.getFont().getSize() - 1));
        executeSafeCommandsDesc.setForeground(Color.GRAY);

        executeAllCommands = new JBCheckBox("执行终端所有命令");
        executeAllCommands.setSelected(actions.getExecuteAllCommands());
        executeAllCommandsDesc = new JLabel("（允许Jarvis执行除黑名单外的所有终端命令）");
        executeAllCommandsDesc.setFont(executeAllCommandsDesc.getFont().deriveFont(Font.PLAIN, executeAllCommandsDesc.getFont().getSize() - 1));
        executeAllCommandsDesc.setForeground(Color.GRAY);

        // 黑名单
        blacklistDesc = new JLabel("黑名单（以下关键字开头的命令不会被自动执行）");
        blackText = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Color.GRAY);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString("输入命令后点击添加", 5, getHeight() / 2 + 5);
                    g2.dispose();
                }
            }
        };
        blackText.setPreferredSize(new Dimension(150, 30));
        blackAddButton = new JButton("添加");
        blacklistCommands = new TagInputField();
        
        blackAddButton.addActionListener(e -> {
            String text = blackText.getText().trim();
            if (!text.isEmpty()) {
                blacklistCommands.addTag(text);
                blackText.setText("");
            }
        });
        // 移除 blacklistCommands 的 setPreferredSize，因为现在由 JScrollPane 控制大小
        blacklistCommands.setTags(state.getAutoRunCommandsBlacklist());

        executeAllCommands.addActionListener(e -> {
            blacklistCommands.setEnabled(executeAllCommands.isSelected());
        });

        blacklistCommands.setEnabled(actions.getExecuteAllCommands());

        useMCPServer = new JBCheckBox("使用 MCP 服务器");
        useMCPServer.setSelected(actions.getUseMcp());
        useMCPServerDesc = new JLabel("（允许Jarvis使用已配置MCP服务器，可能会修改文件系统或与API交互）");
        useMCPServerDesc.setFont(useMCPServerDesc.getFont().deriveFont(Font.PLAIN, useMCPServerDesc.getFont().getSize() - 1));
        useMCPServerDesc.setForeground(Color.GRAY);

        runTask = new JBCheckBox("执行 Task 工具");
        runTask.setSelected(actions.getRunTask());
        runTaskDesc = new JLabel("（允许 Jarvis 创建 subAgent 子任务来处理复杂问题）");
        runTaskDesc.setFont(runTaskDesc.getFont().deriveFont(Font.PLAIN, runTaskDesc.getFont().getSize() - 1));
        runTaskDesc.setForeground(Color.GRAY);

        runSkill = new JBCheckBox("使用 Skill 工具");
        runSkill.setSelected(actions.getRunSkill());
        runSkillDesc = new JLabel("（允许 Jarvis 使用技能工具来学习外部技能）");
        runSkillDesc.setFont(runSkillDesc.getFont().deriveFont(Font.PLAIN, runSkillDesc.getFont().getSize() - 1));
        runSkillDesc.setForeground(Color.GRAY);

        // 最大请求数
        maxRequestsDesc = new JLabel("单次对话中允许自动批准的最大数量：");
        maxRequestsDesc.setToolTipText("Jarvis在单次对话中允许自动批准的最大数量，超过此数量Jarvis将要求用户手动批准。下次对话将重新开始计数。");
        maxRequests = new JBTextField("100", 10);
        maxRequests.setText(String.valueOf(state.getMaxRequests()));
        maxRequests.setToolTipText("Jarvis在单次对话中允许自动批准的最大数量,超过此数量Jarvis将要求用户手动批准。下次对话将重新开始计数。");


    }

    private void operationSetting(JPanel mainPanel) {

        // 添加操作选项
        JPanel rowPanel = createOperateModeRow();

        mainPanel.add(rowPanel);
    }

    private JPanel createOperateModeRow() {
        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 30, 0)); // 1行2列，列间距30

        // 左列面板
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(new TitledSeparator("快速设置"));

        // 基础操作
        JPanel maxRequestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        maxRequestPanel.add(maxRequestsDesc);
        maxRequestPanel.add(maxRequests);
        leftPanel.add(maxRequestPanel);

        JPanel readProjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        readProjectPanel.add(readProjectFiles);
        readProjectPanel.add(readProjectFilesDesc);
        leftPanel.add(readProjectPanel);

        JPanel readAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        readAllPanel.add(Box.createHorizontalStrut(20));
        readAllPanel.add(readAllFiles);
        readAllPanel.add(readAllFilesDesc);
        leftPanel.add(readAllPanel);

        JPanel editProjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        editProjectPanel.add(editProjectFiles);
        editProjectPanel.add(editProjectFilesDesc);
        leftPanel.add(editProjectPanel);

        JPanel safeCmdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        safeCmdPanel.add(executeSafeCommands);
        safeCmdPanel.add(executeSafeCommandsDesc);
        leftPanel.add(safeCmdPanel);

        JPanel allCmdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        allCmdPanel.add(Box.createHorizontalStrut(20));
        allCmdPanel.add(executeAllCommands);
        allCmdPanel.add(executeAllCommandsDesc);
        leftPanel.add(allCmdPanel);

        JPanel blacklistPanel = new JPanel();
        blacklistPanel.setLayout(new BoxLayout(blacklistPanel, BoxLayout.Y_AXIS));
        JPanel blacklistLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        blacklistLabelPanel.add(Box.createHorizontalStrut(21));
        blacklistLabelPanel.add(blacklistDesc);
        blacklistLabelPanel.add(blackText);
        blacklistLabelPanel.add(blackAddButton);
        blacklistPanel.add(blacklistLabelPanel);
        JPanel blacklistFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        blacklistFieldPanel.add(Box.createHorizontalStrut(21));

        // 直接添加黑名单组件，无滚动条，无限高度展开
        blacklistFieldPanel.add(blacklistCommands);
        blacklistPanel.add(blacklistFieldPanel);
        leftPanel.add(blacklistPanel);

        JPanel mcpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        mcpPanel.add(useMCPServer);
        mcpPanel.add(useMCPServerDesc);
        leftPanel.add(mcpPanel);

        JPanel taskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        taskPanel.add(runTask);
        taskPanel.add(runTaskDesc);
        leftPanel.add(taskPanel);

        JPanel skillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        skillPanel.add(runSkill);
        skillPanel.add(runSkillDesc);
        leftPanel.add(skillPanel);


        rowPanel.add(leftPanel);

        return rowPanel;
    }



    @Override
    public boolean isModified() {
        AutoApproveSettings.State state = AutoApproveSettings.Companion.getState();
        AutoApproveSettings.State.Actions actions = state.getActions();
        if (!maxRequests.getText().equals(String.valueOf(state.getMaxRequests()))) return true;

        if (readAllFiles.isSelected() != actions.getReadFilesExternally()) return true;
        if (editProjectFiles.isSelected() != actions.getEditFiles()) return true;
        if (executeSafeCommands.isSelected() != actions.getExecuteSafeCommands()) return true;
        if (executeAllCommands.isSelected() != actions.getExecuteAllCommands()) return true;
        if (useMCPServer.isSelected() != actions.getUseMcp()) return true;
        if (runTask.isSelected() != actions.getRunTask()) return true;
        if (runSkill.isSelected() != actions.getRunSkill()) return true;

        List<String> currentBlacklist = blacklistCommands.getTags();
        List<String> savedBlacklist = state.getAutoRunCommandsBlacklist();
        if (!currentBlacklist.equals(savedBlacklist)) return true;

        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        AutoApproveSettings.State state = AutoApproveSettings.Companion.getState();
        AutoApproveSettings.State.Actions actions = state.getActions();

        try {
            int maxRequestsValue = Integer.parseInt(maxRequests.getText());
            if (maxRequestsValue < 1) {
                throw new ConfigurationException("最大请求数必须大于0");
            }
            state.setMaxRequests(maxRequestsValue);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("最大请求数必须是有效的数字");
        }

        actions.setReadFilesExternally(readAllFiles.isSelected());
        actions.setEditFiles(editProjectFiles.isSelected());
        actions.setExecuteSafeCommands(executeSafeCommands.isSelected());
        actions.setExecuteAllCommands(executeAllCommands.isSelected());
        actions.setUseMcp(useMCPServer.isSelected());
        actions.setRunTask(runTask.isSelected());
        actions.setRunSkill(runSkill.isSelected());

        state.getAutoRunCommandsBlacklist().clear();
        state.getAutoRunCommandsBlacklist().addAll(blacklistCommands.getTags());
    }

    @Override
    public void reset() {
        AutoApproveSettings.State state = AutoApproveSettings.Companion.getState();
        AutoApproveSettings.State.Actions actions = state.getActions();

        maxRequests.setText(String.valueOf(state.getMaxRequests()));

        readProjectFiles.setSelected(actions.getReadFiles());
        readAllFiles.setSelected(actions.getReadFilesExternally());
        editProjectFiles.setSelected(actions.getEditFiles());
        executeSafeCommands.setSelected(actions.getExecuteSafeCommands());
        executeAllCommands.setSelected(actions.getExecuteAllCommands());
        useMCPServer.setSelected(actions.getUseMcp());
        runTask.setSelected(actions.getRunTask());
        runSkill.setSelected(actions.getRunSkill());

        blacklistCommands.setTags(state.getAutoRunCommandsBlacklist());
    }
}
