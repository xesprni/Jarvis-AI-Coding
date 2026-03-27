package com.qihoo.finance.lowcode.smartconversation.conversations;

import com.intellij.icons.AllIcons;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.qifu.agent.parser.*;
import com.qifu.ui.smartconversation.editor.action.DiffHeaderPanel;
import com.qifu.ui.smartconversation.panels.*;
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService;
import com.qifu.ui.smartconversation.textarea.WebpageList;
import com.qifu.ui.smartconversation.textarea.event.AnalysisCompletedEventDetails;
import com.qifu.ui.smartconversation.textarea.event.AnalysisFailedEventDetails;
import com.qifu.ui.smartconversation.textarea.event.EventDetails;
import com.qifu.ui.smartconversation.textarea.event.WebSearchEventDetails;
import com.qifu.ui.utils.EditorUtil;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.UIUtil;
import com.qihoo.finance.lowcode.smartconversation.actions.CopyAction;
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType;
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType;
import kotlin.jvm.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.Objects;

import static com.qifu.ui.utils.MarkdownUtil.convertMdToErrorHtml;
import static com.qifu.ui.utils.MarkdownUtil.convertMdToHtml;
import static java.lang.String.format;
import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;

public class TaskMessageResponseBody extends JPanel {

    private static final Logger LOG = Logger.getInstance(TaskMessageResponseBody.class);

    private final Project project;
    private final Disposable parentDisposable;
    private final SseMessageParser streamOutputParser;
    private final boolean readOnly;
    private final DefaultListModel<WebSearchEventDetails> webpageListModel = new DefaultListModel<>();
    private final WebpageList webpageList = new WebpageList(webpageListModel);
    private final ResponseBodyProgressPanel progressPanel = new ResponseBodyProgressPanel();
    private final JPanel loadingLabel = createLoadingPanel();
    public final JPanel contentPanel =
            new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 4, true, false));

    private ResponseEditorPanel currentlyProcessedEditorPanel;
    private JEditorPane currentlyProcessedTextPane;
    private JEditorPane currentlyProcessedErrorPane;
    private JPanel webpageListPanel;
    private final ToolPanelFactory toolPanelFactory;

    private String lastCommandContent;
    private JPanel planActionPanel;

    private JPanel createLoadingPanel() {
        return new BorderLayoutPanel()
                .addToLeft(new JBLabel(
                        "Generating response...",
                        new AnimatedIcon.Default(),
                        JLabel.LEFT))
                .withBackground(Constants.Color.PANEL_BACKGROUND)
                .withBorder(JBUI.Borders.empty(4, 0));
    }

    public TaskMessageResponseBody(Project project, Disposable parentDisposable) {
        this(project, false, false, false, false, parentDisposable);
    }

    public TaskMessageResponseBody(
            Project project,
            boolean readOnly,
            boolean webSearchIncluded,
            boolean withProgress,
            boolean withLoading,
            Disposable parentDisposable) {
        this.project = project;
        this.parentDisposable = parentDisposable;
        this.streamOutputParser = new SseMessageParser();
        this.readOnly = readOnly;

        setLayout(new BorderLayout());
        setOpaque(false);

        contentPanel.setOpaque(false);
        add(contentPanel, BorderLayout.NORTH);

        loadingLabel.setVisible(withLoading);
        add(loadingLabel, BorderLayout.SOUTH);

        if (ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) == ServiceType.PROXYAI) {
            if (withProgress) {
                contentPanel.add(progressPanel);
            }

            if (webSearchIncluded) {
                webpageListPanel = createWebpageListPanel(webpageList);
                contentPanel.add(webpageListPanel);
            }
        }
        this.toolPanelFactory = new ToolPanelFactory(project);
    }

    public void showPlanActions(Runnable onExecuteAgent, Runnable onContinuePlan) {
        if (onExecuteAgent == null && onContinuePlan == null) {
            return;
        }
        if (planActionPanel == null) {
            planActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            planActionPanel.setOpaque(false);
            contentPanel.add(planActionPanel);
        } else {
            planActionPanel.removeAll();
        }

        planActionPanel.add(createPlanActionButton("按计划执行", onExecuteAgent));
        planActionPanel.add(createPlanActionButton("继续编辑计划", onContinuePlan));

        planActionPanel.revalidate();
        planActionPanel.repaint();
    }

    private JButton createPlanActionButton(String text, Runnable action) {
        var button = new JButton(text);
        button.setEnabled(action != null);
        button.addActionListener(e -> {
            if (action != null && button.isEnabled()) {
                button.setEnabled(false);
                action.run();
            }
        });
        return button;
    }


    public void withResponse(@NotNull Segment segment) {
        try {
            processResponse(segment, false);
        } catch (Exception e) {
            LOG.warn("Something went wrong while processing input", e);
        }
    }

    public void withToolResponse(String eventId, @NotNull Segment segment, boolean isPartial) {
        try {
            processToolResponse(eventId, segment, isPartial);
        } catch (Exception e) {
            LOG.warn("Something went wrong while processing input", e);
        }
    }

    public TaskMessageResponseBody withResponse(@NotNull String response) {
        try {
            for (var item : new CompleteMessageParser().parse(response)) {
                processResponse(item, false);
                currentlyProcessedTextPane = null;
                currentlyProcessedEditorPanel = null;
                currentlyProcessedErrorPane = null;
            }
        } catch (Exception e) {
            LOG.warn("Something went wrong while processing input", e);
        }
        return this;
    }

    public void stopLoading() {
        loadingLabel.setVisible(false);
    }

    public void startLoading() {
        loadingLabel.setVisible(true);
    }


    public void displayQuotaExceeded() {
        String message = "You exceeded your current quota, please check your plan and billing details, "
                + "or <a href=\"#CHANGE_PROVIDER\">change</a> to a different LLM provider.";
        displayErrorMessage(message, e -> {
            if (e.getEventType() == ACTIVATED) {
//        ShowSettingsUtil.getInstance()
//            .showSettingsDialog(project, GeneralSettingsConfigurable.class);
            }
        });
    }

    public void displayError(String message) {
        displayErrorMessage(message, null);
    }

    public void hideCaret() {
        if (currentlyProcessedTextPane != null) {
            currentlyProcessedTextPane.getCaret().setVisible(false);
        }
        if (currentlyProcessedErrorPane != null) {
            currentlyProcessedErrorPane.getCaret().setVisible(false);
        }
    }

    /**
     * 流式渲染结束后，重新处理所有包含表格的 JEditorPane，为表格添加横向滚动条
     * 遍历 contentPanel 中的所有 JEditorPane，检查其保存的原始 HTML 是否包含表格，
     * 如果包含则替换为带横向滚动的组件
     */
    public void rebuildTablesWithScrollPane() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 收集需要处理的组件信息
                var componentsToProcess = new java.util.ArrayList<ComponentInfo>();
                
                for (int i = 0; i < contentPanel.getComponentCount(); i++) {
                    var component = contentPanel.getComponent(i);
                    
                    // 只处理直接添加的 JEditorPane（不是已经在 JScrollPane 中的）
                    if (component instanceof JEditorPane textPane) {
                        // 使用保存的原始 HTML，而不是 getText()
                        Object originalHtmlObj = textPane.getClientProperty("originalHtml");
                        if (originalHtmlObj instanceof String originalHtml && originalHtml.contains("<table")) {
                            componentsToProcess.add(new ComponentInfo(i, textPane, originalHtml));
                        }
                    }
                }
                
                // 如果没有需要处理的表格，直接返回
                if (componentsToProcess.isEmpty()) {
                    return;
                }
                
                // 从后往前处理，避免索引变化问题
                for (int j = componentsToProcess.size() - 1; j >= 0; j--) {
                    var info = componentsToProcess.get(j);
                    rebuildSingleTextPaneWithTables(info.index, info.textPane, info.htmlContent);
                }
                
                // 清除当前处理的 textPane 引用，因为已经被替换了
                currentlyProcessedTextPane = null;
                
                contentPanel.revalidate();
                contentPanel.repaint();
            } catch (Exception e) {
                LOG.warn("Error while rebuilding tables with scroll pane", e);
            }
        });
    }
    
    /**
     * 组件信息记录类
     */
    private static class ComponentInfo {
        final int index;
        final JEditorPane textPane;
        final String htmlContent;
        
        ComponentInfo(int index, JEditorPane textPane, String htmlContent) {
            this.index = index;
            this.textPane = textPane;
            this.htmlContent = htmlContent;
        }
    }
    
    /**
     * 重新构建单个包含表格的 JEditorPane
     */
    private void rebuildSingleTextPaneWithTables(int index, JEditorPane originalTextPane, String html) {
        // 获取原始的 eventId
        Object eventId = originalTextPane.getClientProperty("eventId");
        
        // 移除原始组件
        contentPanel.remove(index);
        
        // 拆分 HTML 内容
        var segments = splitHtmlByTables(html);
        
        // 创建一个临时面板来容纳拆分后的组件
        var containerPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 4, true, false));
        containerPanel.setOpaque(false);
        
        for (var segment : segments) {
            if (segment.isTable) {
                // 表格部分：创建 JTextPane 并用 JScrollPane 包裹
                var textPane = createTextPane(wrapHtmlBody(segment.content), false);
                textPane.putClientProperty("eventId", eventId);
                textPane.getCaret().setVisible(false);
                
                var scrollPane = ScrollPaneFactory.createScrollPane(textPane);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                scrollPane.setBorder(null);
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);
                containerPanel.add(scrollPane);
            } else {
                // 非表格部分：正常添加
                var textPane = createTextPane(wrapHtmlBody(segment.content), false);
                textPane.putClientProperty("eventId", eventId);
                textPane.getCaret().setVisible(false);
                containerPanel.add(textPane);
            }
        }
        
        // 将容器面板添加到原来的位置
        contentPanel.add(containerPanel, index);
    }

    public void clear() {
        contentPanel.removeAll();
        streamOutputParser.clear();
        loadingLabel.setVisible(false);
        
        // Reset text pane references
        currentlyProcessedTextPane = null;
        currentlyProcessedEditorPanel = null;
        currentlyProcessedErrorPane = null;

        repaint();
        revalidate();
    }

    private void displayErrorMessage(String message, HyperlinkListener hyperlinkListener) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (loadingLabel.isVisible()) {
                loadingLabel.setVisible(false);
            }
            if (webpageListPanel != null) {
                webpageListPanel.setVisible(false);
            }

            String formattedMessage = format(
                    "<html><p style=\"margin-top: 4px; margin-bottom: 8px;\">%s</p></html>", message);

            if (currentlyProcessedTextPane == null) {
                currentlyProcessedTextPane = createTextPane(formattedMessage, false);
                contentPanel.add(currentlyProcessedTextPane);
            }

            currentlyProcessedTextPane.setVisible(true);
            currentlyProcessedTextPane.setText(formattedMessage);

            if (hyperlinkListener != null) {
                for (HyperlinkListener listener : currentlyProcessedTextPane.getHyperlinkListeners()) {
                    currentlyProcessedTextPane.removeHyperlinkListener(listener);
                }
                currentlyProcessedTextPane.addHyperlinkListener(hyperlinkListener);
            }

            hideCaret();

            revalidate();
            repaint();
        });
    }


    @Synchronized
    private void processToolResponse(String eventId, Segment item, boolean isPartial) {
        if (item instanceof ToolSegment toolSegment) {
            String markdownText = toolSegment.getToolContent();
            UiToolName name = toolSegment.getName();
            if(UiToolName.COMMAND_OUTPUT.equals(toolSegment.getName())){
                name = UiToolName.RUN_COMMAND;
            }
            ToolPanel toolPanel = this.toolPanelFactory.getPanel(eventId);
            if (toolPanel == null) {
                toolPanel = this.toolPanelFactory.createPanel(eventId, name, toolSegment);
                prepareProcessingTool(toolPanel);
            }
            ToolHeader toolHeader = SegmentKt.getToolSegmentHeader(toolSegment);
            toolPanel.setContent(toolSegment.getToolCommand(), markdownText, toolSegment.getParams(), toolHeader, isPartial);
        }
    }

    private void processResponse(Segment item, boolean caretVisible) {

        if (item instanceof CodeEnd) {
            if (currentlyProcessedEditorPanel != null) {
                handleHeaderOnCompletion(currentlyProcessedEditorPanel);
            }
            currentlyProcessedEditorPanel = null;
            return;
        }

        if (item instanceof SearchReplace searchReplace) {
            if (currentlyProcessedEditorPanel == null) {
                prepareProcessingCode(searchReplace);
            }
            if (currentlyProcessedEditorPanel != null) {
                currentlyProcessedEditorPanel.handleSearchReplace(searchReplace);
                handleHeaderOnCompletion(currentlyProcessedEditorPanel);
                return;
            }
        }

        if (item instanceof ReplaceWaiting replaceWaiting) {
            if (currentlyProcessedEditorPanel != null) {
                currentlyProcessedEditorPanel.handleReplace(replaceWaiting);
                return;
            }
        }

        if (item instanceof Code || item instanceof SearchWaiting) {
            processCode(item);
            return;
        }

        if (item instanceof TextSegment) {
            processText(item.getContent(), caretVisible, ((TextSegment) item).getEventId());
        }

        if (item instanceof ErrorSegment) {
            processError(item.getContent(), caretVisible);
        }
    }

    private void processCode(Segment item) {
        var content = item.getContent();
        if (currentlyProcessedEditorPanel == null) {
            prepareProcessingCode(item);
            return;
        }

        var editor = currentlyProcessedEditorPanel.getEditor();
        if (item instanceof Code && editor != null) {
            EditorUtil.updateEditorDocument(editor, content);
        }
    }

    private void processText(String markdownText, boolean caretVisible, String eventId) {
        if (markdownText == null || markdownText.isEmpty()) {
            return;
        }

        var html = convertMdToHtml(markdownText);
        
        if (currentlyProcessedTextPane == null || currentlyProcessedTextPane.getClientProperty("eventId") != eventId) {
            // 首次创建：检查是否包含表格来决定渲染方式
            boolean containsTable = html.contains("<table");
            if (containsTable) {
                // 包含表格：使用细粒度渲染（拆分表格和非表格内容）
                prepareProcessingTextWithTables(caretVisible, eventId, html);
            } else {
                // 不包含表格：普通渲染
                prepareProcessingText(caretVisible, eventId);
                currentlyProcessedTextPane.setText(html);
                // 保存原始 HTML 以便后续重建表格时使用
                currentlyProcessedTextPane.putClientProperty("originalHtml", html);
            }
        } else {
            // 流式更新
            currentlyProcessedTextPane.setText(html);
            // 更新保存的原始 HTML
            currentlyProcessedTextPane.putClientProperty("originalHtml", html);
        }
    }


    private void processError(String errorMessage, boolean caretVisible) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return;
        }
        var html = convertMdToErrorHtml(errorMessage);
        prepareProcessingError(caretVisible);
        currentlyProcessedErrorPane.setText(html);
    }

    @Synchronized
    private void prepareProcessingText(boolean caretVisible, String eventId) {
        currentlyProcessedEditorPanel = null;
        this.toolPanelFactory.removeAllPanel();
        currentlyProcessedTextPane = createTextPane("", caretVisible);
        currentlyProcessedTextPane.putClientProperty("eventId", eventId);
        contentPanel.add(currentlyProcessedTextPane);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * 处理包含表格的内容：将表格拆分出来单独用 JScrollPane 包裹
     * 只用于首次加载（如历史记录），流式渲染不适用此方法
     */
    @Synchronized
    private void prepareProcessingTextWithTables(boolean caretVisible, String eventId, String html) {
        currentlyProcessedEditorPanel = null;
        currentlyProcessedTextPane = null;
        this.toolPanelFactory.removeAllPanel();
        
        // 使用正则表达式拆分表格和非表格内容
        var segments = splitHtmlByTables(html);
        
        for (var segment : segments) {
            if (segment.isTable) {
                // 表格部分：创建 JTextPane 并用 JScrollPane 包裹
                var textPane = createTextPane(wrapHtmlBody(segment.content), caretVisible);
                textPane.putClientProperty("eventId", eventId);
                
                var scrollPane = ScrollPaneFactory.createScrollPane(textPane);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                scrollPane.setBorder(null);
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);
                contentPanel.add(scrollPane);
            } else {
                // 非表格部分：正常添加
                var textPane = createTextPane(wrapHtmlBody(segment.content), caretVisible);
                textPane.putClientProperty("eventId", eventId);
                contentPanel.add(textPane);
            }
        }
        
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * 包裹 HTML 片段为完整的 HTML 文档
     */
    private String wrapHtmlBody(String content) {
        return "<html><body>" + content + "</body></html>";
    }

    /**
     * 按表格拆分 HTML 内容
     */
    private java.util.List<HtmlSegment> splitHtmlByTables(String html) {
        var segments = new java.util.ArrayList<HtmlSegment>();
        
        // 移除 <html> 和 <body> 标签，只保留内容
        String body = html.replaceAll("(?i)</?html>", "").replaceAll("(?i)</?body>", "").trim();
        
        // 使用正则表达式匹配表格
        var pattern = java.util.regex.Pattern.compile("(<table[^>]*>.*?</table>)", 
                                                      java.util.regex.Pattern.DOTALL | 
                                                      java.util.regex.Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(body);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // 添加表格前的内容
            if (matcher.start() > lastEnd) {
                String beforeTable = body.substring(lastEnd, matcher.start()).trim();
                if (!beforeTable.isEmpty()) {
                    segments.add(new HtmlSegment(beforeTable, false));
                }
            }
            
            // 添加表格
            String table = matcher.group(1);
            segments.add(new HtmlSegment(table, true));
            lastEnd = matcher.end();
        }
        
        // 添加最后一个表格后的内容
        if (lastEnd < body.length()) {
            String afterTable = body.substring(lastEnd).trim();
            if (!afterTable.isEmpty()) {
                segments.add(new HtmlSegment(afterTable, false));
            }
        }
        
        return segments;
    }

    /**
     * HTML 片段：表示内容是否为表格
     */
    private static class HtmlSegment {
        final String content;
        final boolean isTable;
        
        HtmlSegment(String content, boolean isTable) {
            this.content = content;
            this.isTable = isTable;
        }
    }




    @Synchronized
    private void prepareProcessingTool(ToolPanel toolPanel) {
        currentlyProcessedEditorPanel = null;
        currentlyProcessedTextPane = null;
        currentlyProcessedErrorPane = null;
        contentPanel.add(toolPanel);
        contentPanel.revalidate();
        contentPanel.repaint();

    }

    @Synchronized
    private void prepareProcessingError(boolean caretVisible) {
        currentlyProcessedTextPane = null;
        currentlyProcessedEditorPanel = null;
        this.toolPanelFactory.removeAllPanel();
        currentlyProcessedErrorPane = createTextPane("", caretVisible);
        contentPanel.add(currentlyProcessedErrorPane);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    @Synchronized
    private void prepareProcessingCode(Segment item) {
        hideCaret();
        currentlyProcessedTextPane = null;
        currentlyProcessedEditorPanel =
                new ResponseEditorPanel(project, item, readOnly, parentDisposable);
        this.toolPanelFactory.removeAllPanel();
        contentPanel.add(currentlyProcessedEditorPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void handleHeaderOnCompletion(ResponseEditorPanel editorPanel) {
        var editor = editorPanel.getEditor();
        if (editor != null) {
            var header = editor.getPermanentHeaderComponent();
            if (header instanceof DiffHeaderPanel diffHeaderPanel) {
                diffHeaderPanel.handleDone();
            } else if (header instanceof DefaultHeaderPanel defaultHeaderPanel) {
                defaultHeaderPanel.handleDone();
            }
        }
    }

    private void displayWebSearchItem(WebSearchEventDetails details) {
        webpageListModel.addElement(details);
        webpageList.revalidate();
        webpageList.repaint();
    }

    private void showWebDocsProgress() {
        progressPanel.updateProgressContainer(
                "Analyzing web content",
                null
        );
    }

    private void completeWebDocsProgress(EventDetails eventDetails) {
        if (eventDetails instanceof AnalysisCompletedEventDetails defaultEventDetails) {
            progressPanel.updateProgressContainer(
                    defaultEventDetails.getDescription(),
                    Icons.GreenCheckmark);
        }
    }

    private void failWebDocsProgress(EventDetails eventDetails) {
        if (eventDetails instanceof AnalysisFailedEventDetails failedEventDetails) {
            progressPanel.updateProgressContainer(failedEventDetails.getError(), General.Error);
        }
    }

    private JTextPane createTextPane(String text, boolean caretVisible) {
        var textPane = UIUtil.createTextPane(text, false, event -> {
            if (FileUtil.exists(event.getDescription()) && ACTIVATED.equals(event.getEventType())) {
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(event.getDescription());
                FileEditorManager.getInstance(project).openFile(Objects.requireNonNull(file), true);
                return;
            }

            UIUtil.handleHyperlinkClicked(event);
        });
        if (caretVisible) {
            textPane.getCaret().setVisible(true);
            textPane.setCaretPosition(textPane.getDocument().getLength());
        }
        textPane.setBorder(JBUI.Borders.empty());

        installPopupMenu(textPane);

        return textPane;
    }

    private void installPopupMenu(JTextPane textPane) {
        PopupHandler.installPopupMenu(textPane, new DefaultActionGroup(
                new AnAction(
                        "Copy",
                        "Copy to clipboard",
                        AllIcons.Actions.Copy) {

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }

                    @Override
                    public void actionPerformed(@NotNull AnActionEvent event) {
                        textPane.copy();
                        CopyAction.showCopyBalloon(event);
                    }

                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        e.getPresentation().setEnabled(textPane.getSelectedText() != null);
                    }
                }
        ), ActionPlaces.EDITOR_POPUP);
    }

    private static JPanel createWebpageListPanel(WebpageList webpageList) {
        var title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.setBorder(JBUI.Borders.empty(8, 0));
        title.add(new JBLabel("WEB PAGES")
                .withFont(JBUI.Fonts.miniFont()), BorderLayout.LINE_START);
        var listPanel = new JPanel(new BorderLayout());
        listPanel.add(webpageList, BorderLayout.LINE_START);

        var panel = new JPanel(new BorderLayout());
        panel.add(title, BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    public void toolPanelRemoveAll() {
        this.toolPanelFactory.removeAllPanel();
    }
}
