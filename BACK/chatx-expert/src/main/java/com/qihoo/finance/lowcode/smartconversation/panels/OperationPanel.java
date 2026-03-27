package com.qihoo.finance.lowcode.smartconversation.panels;

import com.intellij.openapi.project.Project;
import com.qifu.ui.smartconversation.panels.DiffWindowHolder;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qifu.ui.smartconversation.sse.EventSourceToAgentAdapter;
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation;
import com.qihoo.finance.lowcode.smartconversation.conversations.Message;
import com.qihoo.finance.lowcode.smartconversation.service.TaskCompletionRequestHandler;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class OperationPanel extends JPanel {
    private final Project project;
    private String taskId;

    @Getter
    @Setter
    private TaskCompletionRequestHandler requestHandler;

    public OperationPanel(Project project) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setOpaque(false);
        createRootPanel();
        this.project = project;
        this.updateVisibility(false);
    }


    private void createRootPanel() {
        // 右侧按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttonPanel.setOpaque(false);

        JButton approveButton = new JButton("批准");
        JButton rejectButton = new JButton("拒绝");
        approveButton.addActionListener(this::onApprove);
        rejectButton.addActionListener(this::onReject);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);

        // 添加按钮面板到右侧
        add(buttonPanel, BorderLayout.EAST);
    }


    // 新增方法：根据变量控制按钮显示
    public void updateVisibility(boolean isVisible) {
        this.setVisible(isVisible);
        // 更新布局
        revalidate();
        repaint();
    }

    public void waitUserInput(String taskId) {
        this.taskId = taskId;
    }


    // 提供按钮事件注册方法
    public void onApprove(ActionEvent event) {
        // 1. 收集用户修改代码
        String userInput;
        if(DiffWindowHolder.INSTANCE.isModified(taskId)) {
            // 获取用户修改的代码 给UI展示用的
            String userEdit = DiffWindowHolder.INSTANCE.showUserModifications(taskId, false);
            // 获取整体修改的代码 给工具(Edit、write)上下文用的
            String allEdit = DiffWindowHolder.INSTANCE.showUserModifications(taskId, true);
            Map<String, String> map = new HashMap<>();
            map.put("userEdit", userEdit);
            map.put("allEdit", allEdit);
            userInput = "approve" + JSON.toJSONString(map);
            // 应用用户修改的代码
            DiffWindowHolder.INSTANCE.applyUserModifications(taskId);
        } else {
            // 原有批准类型
            userInput = "approve";
        }

        EventSourceToAgentAdapter.INSTANCE.getChatCompletionAsync(buildParams(taskId, userInput), null, project);
        this.updateVisibility(false);
    }

    public void onReject(ActionEvent event) {
        EventSourceToAgentAdapter.INSTANCE.getChatCompletionAsync(buildParams(taskId, "reject"), null, project);
        this.updateVisibility(false);
    }

    private TaskCompletionParameters buildParams(String taskId, String userInput) {
        Message message = new Message();
        message.setPrompt(userInput);
        Conversation conversation = new Conversation();
        conversation.setTaskId(taskId);
        return TaskCompletionParameters.builder(taskId, message).hasCustomInput(false).build();
    }
}