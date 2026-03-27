package com.qihoo.finance.lowcode.settings.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.layout.MyBoxLayout;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ChatxApplicationState;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class CodeCompletionSettingForm implements Configurable, Disposable {

    public static final String NAME = "AI参数配置";
    private static final Option<String>[] LINE_MODES = new Option[]{
            new Option<>("server", "智能"),
            new Option<>("linebyline", "单行"),
            new Option<>("block", "多行")
    };

    private static final Option<String>[] MODELS = new Option[]{
            new Option<>("codegeex-lite", "基础版"),
            new Option<>("codegeex-pro", "专业版"),
            new Option<>("DIFY", "Dify"),
    };

    private ComboBox<Option<String>> lineModeComboBox;
    private ComboBox<Option<String>> modelComboBox;
    private ComboBox<Option<String>> datasetsComboBox;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return NAME;
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel mainPanel = new JPanel();

        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        MyBoxLayout boxLayout = new MyBoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        // AskAi setting
        askAiSetting(mainPanel, settings);
        // code complete setting
        codeCompleteSetting(mainPanel, settings);
        // statement
        TitledSeparator separator = new TitledSeparator("声明");
        separator.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        mainPanel.add(separator);
        mainPanel.add(statement());

        return mainPanel;
    }

    private void askAiSetting(JPanel mainPanel, ChatxApplicationState settings) {
        mainPanel.add(new TitledSeparator("AI问答设置"));
//        mainPanel.add(datasetsSetting(decorator, settings));
        JComponent datasetsPanel = datasetsInfo(settings);
        mainPanel.add(JPanelUtils.settingPanel("知识库:  ", datasetsPanel));
    }

    private JComponent datasetsInfo(ChatxApplicationState settings) {
        JPanel shortcutContent = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JPanel datasetPanel = new JPanel(new GridLayout(-1, 2));
        shortcutContent.add(datasetPanel);

        List<String> datasetList = settings.datasets.stream()
                .map(DatasetInfo::getDatasetName).toList();
        for (String dataset : datasetList) {
            JLabel datasetLabel = new JLabel(dataset);
            datasetLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            datasetPanel.add(datasetLabel);
        }

        JPanel content = new JPanel();
        content.add(shortcutContent);

//        Border paddingBorder = BorderFactory.createEmptyBorder(25, 15, 25, 15);
//        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
//        content.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        content.setBorder(null);
        return content;
    }

    @NotNull
    private static JPanel statement() {
        JPanel rowPanel = new JPanel(new BorderLayout());
        JBTextArea textArea = new JBTextArea();
        textArea.setEditable(false);
        textArea.setBackground(null);
        textArea.setMargin(JBUI.insets(5));
        textArea.setText("使用大模型进行代码补全时，我们需要获取你的代码上下文信息以完成补全，但上下文信息不会被存储或用于其他任何目的，该等数据完全由你所有及控制。\n\n" +
                "使用大模型生成的所有内容均由人工智能模型生成，其生成内容的准确性和完整性无法保证，不代表我们的态度或观点。");
        textArea.setLineWrap(true);
        rowPanel.add(textArea);
        return rowPanel;
    }

    private void codeCompleteSetting(JPanel mainPanel, ChatxApplicationState settings) {
        mainPanel.add(new TitledSeparator("代码补全设置"));
        JPanel rowPanel = createLineModeRow(settings);
        mainPanel.add(rowPanel);
        // 2024/04/28 不再展示模型
//        rowPanel = createModelRow(settings);
//        mainPanel.add(rowPanel);
        rowPanel = new JPanel(new BorderLayout());
        String text = "部分应用：Control+→ 按词应用补全，Control+Alt+→ 按行应用补全";
        if (!SystemInfo.isWindows) {
            text = "部分应用：Command+→ 按词应用补全，Command+Control+→ 按行应用补全";
        }
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        rowPanel.add(label);
        mainPanel.add(rowPanel);
        rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowPanel.add(createLinkButton("应用补全详细介绍", e -> {
            BrowserUtil.browse("https://docs.daikuan.qihoo.net/lowcode/user-guides/askai/#%E5%BA%94%E7%94%A8%E8%A1%A5%E5%85%A8");
        }));
        mainPanel.add(rowPanel);
        rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowPanel.add(createLinkButton("补全效果演示", e -> {
            BrowserUtil.browse("https://docs.daikuan.qihoo.net/lowcode/demo/completion/");
        }));
        mainPanel.add(rowPanel);
    }

    private JButton createLinkButton(String text, ActionListener l) {
        JButton button = new JButton("<html><font style=\"color: rgb(88,157,246);\">" + text + "</font></html>");
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new JBInsets(10, -23, 0, 0));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setText("<html><u style=\"color: rgb(88,157,246);\">" + text + "</u></html>");
                button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 设置手型光标
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setText("<html><font style=\"color: rgb(88,157,246);\">" + text + "</font></html>");
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 恢复默认光标
            }
        });
        button.addActionListener(l);
        return button;
    }


    private JPanel createLineModeRow(ChatxApplicationState settings) {
        String title = "补全长度：";
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JBLabel label = new JBLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, -2));
        lineModeComboBox = new ComboBox<>(LINE_MODES);
        setSelectedItem(lineModeComboBox, settings.lineMode);
        label.setLabelFor(lineModeComboBox);
        panel.add(label);
        panel.add(lineModeComboBox);
        return panel;
    }

    @SuppressWarnings("all")
    private JPanel datasetsSetting(LoadingDecorator decorator, ChatxApplicationState settings) {
        String title = "代码知识库：";
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JBLabel label = new JBLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, -2));
        datasetsComboBox = new ComboBox<>(new Option[]{
                new Option<>("", "不使用知识库")
        });
        label.setLabelFor(datasetsComboBox);
        panel.add(label);
        panel.add(datasetsComboBox);

        // loading datasets
        UIUtil.invokeLaterIfNeeded(() -> decorator.startLoading(false));
        new SwingWorker<Map<String, String>, Map<String, String>>() {
            @Override
            protected Map<String, String> doInBackground() throws Exception {
                return ChatUtil.getDatasets(QuestionType.ASK);
            }

            @SneakyThrows
            @Override
            protected void done() {
                Map<String, String> datasets = get();
                UIUtil.invokeLaterIfNeeded(() -> {
                    // dataset item
                    datasets.keySet().stream()
                            .map(datasetId -> new Option<>(datasetId, datasets.get(datasetId)))
                            .forEach(datasetsComboBox::addItem);
                    // selected
                    setSelectedItem(datasetsComboBox, settings.dataset);
                    // stop loading
                    decorator.stopLoading();
                });

                super.done();
            }
        }.execute();
        return panel;
    }

    private JPanel createModelRow(ChatxApplicationState settings) {
        String title = "补全模型：";
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JBLabel label = new JBLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, -2));
        modelComboBox = new ComboBox<>(MODELS);
        setSelectedItem(modelComboBox, settings.model);
        label.setLabelFor(modelComboBox);
        panel.add(label);
        panel.add(modelComboBox);
        return panel;
    }

    private void setSelectedItem(ComboBox<Option<String>> comboBox, String value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Option<String> item = comboBox.getItemAt(i);
            if (item.getValue().equals(value)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public boolean isModified() {
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        String lineMode = ((Option<String>) lineModeComboBox.getSelectedItem()).getValue();
//        String model = ((Option<String>) modelComboBox.getSelectedItem()).getValue();
//        String dataset = ((Option<String>) datasetsComboBox.getSelectedItem()).getValue();
//        return !lineMode.equals(settings.lineMode) || !model.equals(settings.model) || !dataset.equals(settings.dataset);
        return !lineMode.equals(settings.lineMode);
    }

    @Override
    public void apply() throws ConfigurationException {
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        String lineMode = ((Option<String>) lineModeComboBox.getSelectedItem()).getValue();
//        String model = ((Option<String>) modelComboBox.getSelectedItem()).getValue();
//        String dataset = ((Option<String>) datasetsComboBox.getSelectedItem()).getValue();
        if (!lineMode.equals(settings.lineMode)) {
            settings.lineMode = lineMode;
        }
//        if (!model.equals(settings.model)) {
//            settings.model = model;
//        }
//        if (!dataset.equals(settings.dataset)) {
//            settings.dataset = dataset;
//        }
    }

    @Override
    public void reset() {
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        setSelectedItem(lineModeComboBox, settings.lineMode);
//        setSelectedItem(modelComboBox, settings.model);
//        setSelectedItem(datasetsComboBox, settings.dataset);
    }

    @Override
    public void dispose() {

    }
}
