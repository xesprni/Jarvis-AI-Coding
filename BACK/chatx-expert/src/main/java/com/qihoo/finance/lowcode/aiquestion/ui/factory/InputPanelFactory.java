package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.BottomLineBorder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.DropDownLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.TextFieldCompletionProvider;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.component.CancelableButton;
import com.qihoo.finance.lowcode.aiquestion.ui.component.InputEditorTextField;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.stream.StreamMessageBodyFactory;
import com.qihoo.finance.lowcode.aiquestion.ui.view.ActionInstructionPopup;
import com.qihoo.finance.lowcode.aiquestion.ui.view.ConversationHistoryDialog;
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.EditorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.GitIndexUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.FileUpload;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantDetail;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ShortcutInstructionInfo;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.common.enums.GitIndexStatus;
import com.qihoo.finance.lowcode.common.exception.ServiceException;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.kit.ui.KitPopup;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ui.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class InputPanelFactory implements Disposable {

    private final Project project;
    private final static ExecutorService executor = Executors.newFixedThreadPool(10);

    private EditorTextField input;
    private JComponent northPanel;
    private DropDownLink<String> assistantNameLink;
    private JLabel fastCommand;
    private final Icon defaultCommandIcon = AllIcons.General.Add;
    @Setter
    @Getter
    private String conversationId;
    @Setter
    @Getter
    private String lastParamMd5;

    private CancelableButton sendBtn;
    private JLabel progress;
    private JButton nonCommandBtn;
    private JPanel component;
    private NonOpaquePanel northContent;
    private NonOpaquePanel assistantPanel;
    @Getter
    private ComboBox<Option<String>> assistantParam;
    @Getter
    private String assistantToggleValue;
    private String sqlToggleValue;
    private JPanel northToolbar;
    private JLabel assistantIcon;
    private JLabel assistantLabel;
    private final static String INPUT_TIP = "输入\"@\"使用助手,  输入\"#\"查询知识库,  \n输入\"/\"快捷提问";
    public final static String FC_TX = "场景增强";
    private final static int MAX_LINE = 8;
    private ActionListener resetConversationAction;
    // git index
    private String gitToggleValue;
    private JLabel gitInfo;
    private final Icon gitInfoIndexIcon = Icons.scaleToWidth(Icons.REFRESH, 14);
    private final Icon buildIndexIcon = Icons.scaleToWidth(Icons.BUILD_GIT_INDEX, 14);
    private final Icon buildIndexFailIcon = Icons.scaleToWidth(Icons.SYS_ERROR2, 13);
    private String lastGitIndexKey;
    private ScheduledFuture<?> scheduledFuture;
    private ToggleAction gitToggleAction;
    private final AtomicInteger tipsCount = new AtomicInteger(0);
    private final Set<FileUpload> uploadFiles = new HashSet<>();
    private static final JLabel fileUploadTips = new JLabel("+ 拖拽文件或图片添加上下文");

    static {
        fileUploadTips.setForeground(JBColor.GRAY);
        fileUploadTips.setToolTipText("将文件拖拽到对话界面(蓝框内)，即可基于添加的文件进行对话");
    }

    private void resetToggleValue() {
        assistantToggleValue = sqlToggleValue = gitToggleValue = "N";
    }

    private void resetUploadFiles() {
        uploadFiles.clear();
        repaintNorthContent();
    }

    public JPanel create() {
        initEditor();
        // center
        JPanel contentPanel = new JPanel(new BorderLayout());
        Border paddingBorder = BorderFactory.createEmptyBorder(1, 8, 1, 5);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 15);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));

        JScrollPane inputScroll = new JBScrollPane(input.getComponent());
        inputScroll.setOpaque(false);
        inputScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        contentPanel.add(createNorthContent(), BorderLayout.NORTH);
        contentPanel.add(inputScroll, BorderLayout.CENTER);
        contentPanel.add(createSouthMenu(), BorderLayout.SOUTH);

        // north
        northPanel = createNorthMenu();
        northPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        component = new JPanel(new BorderLayout());
        component.add(contentPanel, BorderLayout.CENTER);
        component.add(northPanel, BorderLayout.NORTH);
        component.add(southPanel(), BorderLayout.SOUTH);

        initEvent(inputScroll);
        return component;
    }

    private NonOpaquePanel createNorthContent() {
//        northContent = new NonOpaquePanel(new GridLayout(-1, 2));
        northContent = new NonOpaquePanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
//        northContent.setBorder(BorderFactory.createEmptyBorder(-5, 10, -5, 5));
        repaintNorthContentBorder();
        repaintNorthContent();

        return northContent;
    }

    private void repaintNorthContentBorder() {
        // 创建一个线边框，只有下边线是可见的
        BottomLineBorder lineBorder = new BottomLineBorder();
        // 创建一个空边框，用于设置组件的上下左右的边距
        Border emptyBorder = BorderFactory.createEmptyBorder(-2, 5, -2, 5);
        // 创建一个组合边框，将空边框和线边框组合在一起
        Border compoundBorder = BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        // 将组合边框设置给 northContent 组件
        northContent.setBorder(compoundBorder);
//        if (CollectionUtils.isNotEmpty(uploadFiles)) {
//            Border compoundBorder = BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
//            // 将组合边框设置给 northContent 组件
//            northContent.setBorder(compoundBorder);
//        } else {
//            northContent.setBorder(emptyBorder);
//        }
    }

    public void repaintNorthContent() {
        northContent.removeAll();
        if (!Constants.DEFAULT_ASSISTANT.equalsIgnoreCase(getAssistant())) {
            uploadFiles.clear();
        }

        uploadFiles.stream()
                .map(file -> {
                    JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    JLabel label = new JLabel(file.getName());
                    label.setToolTipText(file.getFullPath());
                    label.setForeground(JBColor.BLUE);
                    filePanel.add(label);
                    // close
                    JButton closeBtn = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.ROLLBACK2, 13), new Dimension(17, 17));
                    closeBtn.addActionListener(e -> {
                        // removeIf 当移除最后一个item时，会NPE，因此需手动调用remove
                        uploadFiles.remove(file);
                        repaintNorthContent();
                        resetConversationAction.actionPerformed(null);
                    });
                    filePanel.add(closeBtn);
                    filePanel.setBorder(BorderFactory.createEmptyBorder(-5, -5, -5, -5));
                    return filePanel;
                }).forEach(fileLabel -> northContent.add(fileLabel));
        if (CollectionUtils.isEmpty(uploadFiles)) {
            northContent.add(fileUploadTips);
        }

        // 互斥清空
        if (CollectionUtils.isNotEmpty(uploadFiles)) resetToggleValue();

        repaintNorthContentBorder();
        northContent.setVisible(Constants.DEFAULT_ASSISTANT.equalsIgnoreCase(getAssistant()));
        northContent.revalidate();
        northContent.repaint();
    }

    public void supportDragAndDropTips(Component dragComponent) {
        new DropTarget(dragComponent, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                clearFileUploadTips(false, 0);
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                super.dragEnter(dtde);
                showFileUploadTips(true);
                clearFileUploadTips(true, 6000);
            }
        });
    }

    public void supportDragAndDrop(Component component) {
        new DropTarget(component, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                clearFileUploadTips(false, 0);
                if (!Constants.DEFAULT_ASSISTANT.equalsIgnoreCase(getAssistant())) return;

                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = new ArrayList<>();
                        List<VirtualFile> virtualFiles = new ArrayList<>();

                        Object transferData = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (transferData instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<File> transferFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                            files.addAll(transferFiles);
                            if ((files.size() + uploadFiles.size()) > FileUpload.MAX_FILE_COUNT) {
                                // 文件数量限制
                                UIUtil.invokeLaterIfNeeded(() -> {
                                    String tips = String.format("会话最多支持 %s 个文件", FileUpload.MAX_FILE_COUNT);
                                    JComponent component = (JComponent) ObjectUtils.defaultIfNull(northPanel, InputPanelFactory.this.component);
                                    NotifyUtils.notifyBalloon(component, tips, Icons.UPDATE, InputPanelFactory.this.component.getForeground(), EditorComponentUtils.BACKGROUND);
                                });
                                return;
                            }
                            if (files.stream().anyMatch(file -> file.length() > FileUpload.MAX_FILE_SIZE)) {
                                // 文件大小限制
                                UIUtil.invokeLaterIfNeeded(() -> {
                                    String tips = "文件大小超过限制, 仅支持小于" + FileUpload.MAX_FILE_SIZE / 1024 / 1024 + "MB的文件";
                                    JComponent component = ObjectUtils.defaultIfNull(northPanel, InputPanelFactory.this.component);
                                    NotifyUtils.notifyBalloon(component, tips, Icons.UPDATE, InputPanelFactory.this.component.getForeground(), EditorComponentUtils.BACKGROUND);
                                });
                                return;
                            }
                        } else {
                            Field[] declaredFields = transferData.getClass().getDeclaredFields();
                            for (Field declaredField : declaredFields) {
                                if (declaredField.getName().contains("psiElements")) {
                                    declaredField.setAccessible(true);
                                    Object field = declaredField.get(transferData);
                                    if (field instanceof PsiElement[] psiElements) {
                                        for (PsiElement psiElement : psiElements) {
                                            PsiFile containingFile = psiElement.getContainingFile();
                                            if (Objects.isNull(containingFile)) continue;
                                            VirtualFile virtualFile = containingFile.getVirtualFile();

                                            virtualFiles.add(virtualFile);
                                        }
                                    }
                                }
                            }
                        }
                        executor.execute(() -> {
                            UIUtil.invokeLaterIfNeeded(() -> setProgress("文件内容读取中..."));
                            for (File file : files) {
                                if (file.isDirectory()) continue;

                                // 文件异步上传, 换取URL后设置到conversation中
                                FileUpload fileUpload = LowCodeAppUtils.uploadFile(file);
                                if (Objects.isNull(fileUpload)) continue;

                                uploadFiles.add(fileUpload);
                                repaintNorthContent();
                            }
                            for (VirtualFile file : virtualFiles) {
                                // 文件异步上传, 换取URL后设置到conversation中
                                FileUpload fileUpload = LowCodeAppUtils.uploadFile(file);
                                if (Objects.isNull(fileUpload)) continue;

                                uploadFiles.add(fileUpload);
                                repaintNorthContent();
                            }
                            UIUtil.invokeLaterIfNeeded(() -> stopProgress());
                        });
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    dtde.dropComplete(false);
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                super.dragEnter(dtde);
                showFileUploadTips(true);
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                super.dragExit(dte);
                clearFileUploadTips(false, 0);
            }
        });
    }

    private void initEvent(JScrollPane inputScroll) {
        // 限高
        input.getDocument().addDocumentListener(resizeDocumentListener(inputScroll));
        // 重置会话action
        resetConversationAction = e -> conversationId = StringUtils.EMPTY;
    }

    private JComponent southPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        // 进度条
        progress = new JLabel();
        progress.setHorizontalAlignment(SwingConstants.LEFT);
        progress.setText(" ");
        progress.setForeground(JBColor.GRAY);
        southPanel.add(progress, BorderLayout.WEST);
        // 代码仓库信息
        gitInfo = new JLabel();
        gitInfo.setHorizontalTextPosition(SwingConstants.LEFT);
        gitInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (!GitIndexUtils.gitIndexStatus(project).getStatus().equals(GitIndexStatus.failed.name())) {
                    return;
                }
                // 点击触发分支索引更新
                try {
                    GitIndexUtils.buildGitIndex(project);
                } catch (ServiceException ex) {
                    NotifyUtils.notifyBalloon(gitInfo, ex.getMessage(), Icons.BUILD_GIT_INDEX, component.getForeground(), EditorComponentUtils.BACKGROUND);
                    return;
                }
                // 状态展示
                gitInfo.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;索引中</html>", GitUtils.getBranchName(project)));
                // gitInfo.setIcon(Icons.LOADING_ANIMATED);
                gitInfo.setToolTipText("点击更新当前分支知识库");
            }
        });
        southPanel.add(gitInfo, BorderLayout.EAST);

        return southPanel;
    }

    public void startProgress() {
        sendBtn.startAction();
        setProgress("AI正在思考中...");
    }

    public void setProgress(String text) {
        progress.setIcon(Icons.LOADING_ANIMATED);
        progress.setText(text);
    }

    public void stopProgress() {
        progress.setIcon(null);
        progress.setText(" ");
    }

    private DocumentListener resizeDocumentListener(JScrollPane inputScroll) {
        return new DocumentListener() {
            private Dimension initDimension;

            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                String text = input.getDocument().getText();
                if (StringUtils.isEmpty(text)) {
                    initDimension = inputScroll.getSize();
                }
                DocumentListener.super.beforeDocumentChange(event);
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                int lineHeight = Objects.requireNonNull(input.getEditor()).getLineHeight();
                int maxHeight = MAX_LINE * lineHeight + 5;
                String text = event.getDocument().getText();
                int height = inputScroll.getSize().height;
                if (countLine(text) >= MAX_LINE && height != initDimension.height) {
                    inputScroll.setPreferredSize(new Dimension(-1, maxHeight));
                } else if (countLine(text) >= MAX_LINE && height <= maxHeight) {
                    // 直接粘贴
                    inputScroll.setPreferredSize(new Dimension(-1, maxHeight));
                } else if (height >= maxHeight && StringUtils.isNotEmpty(text)) {
                    inputScroll.setPreferredSize(new Dimension(-1, maxHeight));
                } else {
                    inputScroll.setPreferredSize(null);
                }
                DocumentListener.super.documentChanged(event);
                CaretModel caretModel = input.getCaretModel();
                int offset = caretModel.getOffset();
                int lineNumber = event.getDocument().getLineNumber(offset);
                if (lineNumber > MAX_LINE) {
                    inputScroll.getVerticalScrollBar().setValue(inputScroll.getVerticalScrollBar().getMaximum());
                }

                component.revalidate();
                component.repaint();
            }
        };
    }

    private int countLine(String str) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }

    private JComponent createNorthMenu() {
        Color background = input.getComponent().getBackground();

        List<String> assistantNames = ChatxApplicationSettings.settings().assistants.stream()
                .map(AssistantInfo::getName).collect(Collectors.toList());
        if (assistantNames.isEmpty()) {
            assistantNames.add(Constants.DEFAULT_ASSISTANT_NAME);
        }
        assistantNameLink = new DropDownLink<String>(assistantNames.get(0), assistantNames);
        assistantNameLink.setBorderPainted(false);
        assistantNameLink.setContentAreaFilled(true);
        assistantNameLink.addItemListener(x -> {
            assistantNameLink.setText((String) x.getItem());
        });

        fastCommand = new JLabel();
        fastCommand.setText(FC_TX);
        fastCommand.setForeground(JBColor.BLUE);
        fastCommand.setIcon(Icons.scaleToWidth(defaultCommandIcon, 13));
        fastCommand.addMouseListener(fastCommandAction());
        fastCommand.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 右边状态栏
        assistantPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
        assistantParam = new ComboBox<>();
        assistantParam.setSwingPopup(false);

        northToolbar = new JPanel();
        gitToggleAction = createGitToggleAction();
        northToolbar.add(createNorthToolbar(createInternetToggleAction(), gitToggleAction, createSQLToggleAction()));
        northToolbar.setBorder(BorderFactory.createEmptyBorder(-7, -7, -7, -10));

        assistantPanel.setBorder(BorderFactory.createEmptyBorder(-3, 0, -3, 0));
        assistantPanel.setVisible(true);

        assistantIcon = new JLabel(Icons.scaleToWidth(Icons.ASSISTANT, 16));
//        assistantLabel = new JLabel(dropDownLink.getText());
        assistantLabel = new RoundedLabel(assistantNameLink.getText(), RoundedLabel.GREEN, RoundedLabel.WHITE, 6);
        assistantPanel.add(assistantIcon);
        assistantPanel.add(assistantLabel);
        // assistantPanel.add(createNonCommand(assistantPanel));
        assistantPanel.add(assistantParam);
        assistantPanel.add(northToolbar);

        // pack
        NonOpaquePanel northMenu = new NonOpaquePanel(new BorderLayout(0, 0));
        northMenu.setOpaque(false);
        northMenu.setBackground(background);
        northMenu.add(assistantPanel, BorderLayout.WEST);
        return northMenu;
    }

    private ToggleAction createSQLToggleAction() {
        return new ToggleAction("SQL模式", "SQL模式", Icons.scaleToWidth(Icons.INTERNET, 20)) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return StringUtils.defaultString("Y").equals(sqlToggleValue);
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                resetToggleValue();
                resetUploadFiles();
                sqlToggleValue = state ? "Y" : "N";
                conversationId = StringUtils.EMPTY;

                if (state) {
                    NotifyUtils.notifyBalloon(northToolbar, "已开启SQL模式，Jarvis 会基于您的问题返回对应的SQL语句",
                            AllIcons.Providers.Mysql, component.getForeground(), EditorComponentUtils.BACKGROUND);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                super.update(event);
                boolean selected = isSelected(event);
                Presentation presentation = event.getPresentation();
                String tips = selected ? "已开启SQL模式，Jarvis 会基于您的问题返回对应的SQL语句" : "已关闭SQL模式";
                presentation.setText(tips);
                presentation.setDescription(tips);
                presentation.setVisible(Constants.DEFAULT_ASSISTANT.equals(getAssistant()));
                presentation.setIcon(selected ? AllIcons.Providers.Mysql : Icons.scaleToWidth(Icons.MY_SQL_DARK, 16));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    private MouseAdapter fastCommandAction() {
        return new MouseAdapter() {
            String commandTxt = null;

            @Override
            public void mouseExited(MouseEvent e) {
                fastCommand.setText(commandTxt);
                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                commandTxt = fastCommand.getText();
                String uTxt = String.format("<html><u style=\"color: rgb(88,157,246);\">%s</u></html>", commandTxt);
                fastCommand.setText(uTxt);
                super.mouseEntered(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                new ActionInstructionPopup(get()).show();
            }
        };
    }

    private JButton createNonCommand(JPanel eastBtnPanel) {
        nonCommandBtn = new JButton();
        nonCommandBtn.setContentAreaFilled(false);
        nonCommandBtn.setBorderPainted(false);
        nonCommandBtn.setIcon(Icons.scaleToWidth(Icons.ROLLBACK2, 13));
        nonCommandBtn.setToolTipText("不使用助手");
        nonCommandBtn.setPreferredSize(new Dimension(16, 16));
        nonCommandBtn.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        nonCommandBtn.addActionListener(e -> {
            // 回到基础助手
            flushAssistant("BASIC_ASSISTANT");
            fastCommand.setText(FC_TX);
            fastCommand.setIcon(Icons.scaleToWidth(defaultCommandIcon, 13));
            eastBtnPanel.setVisible(false);
        });

        return nonCommandBtn;
    }

    private InputPanelFactory get() {
        return this;
    }

    public String getAssistant() {
        if (assistantNameLink != null) {
            String assistantName = assistantNameLink.getText();
            Map<String, String> nameMap = ChatxApplicationSettings.settings().assistants.stream().collect(Collectors.toMap(AssistantInfo::getName, AssistantInfo::getCode));
            if (nameMap.containsKey(assistantName)) {
                return nameMap.get(assistantName);
            }
        }
        return Constants.DEFAULT_ASSISTANT;
    }

    private void initEditor() {
        input = new InputEditorTextField(project, new MyTextFieldCompletionProvider(), "", false, true, false, null, new SendAction());
        input.setToolTipText(INPUT_TIP);
        input.setPlaceholder(INPUT_TIP);
        input.setFileType(FileTypes.PLAIN_TEXT);
        input.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
    }

    private JComponent createSouthMenu() {
        Color background = input.getComponent().getBackground();
        sendBtn = createSendBtn();
        JComponent datasetAction = createInputToolBar();

        JPanel southMenu = new JPanel(new BorderLayout(0, 0));
        southMenu.setBorder(JBUI.Borders.empty(0, 5, 2, -5));
        southMenu.setOpaque(false);
        southMenu.setBackground(background);
        southMenu.add(datasetAction, BorderLayout.EAST);

        NonOpaquePanel westBtnPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
//        westBtnPanel.setBorder(BorderFactory.createEmptyBorder(-7, 0, -8, 0));
//        westBtnPanel.add(fastCommand);
//        westBtnPanel.add(createNonCommand());
        // 工具栏
        westBtnPanel.add(createToolbar());
        southMenu.add(westBtnPanel, BorderLayout.WEST);
        return southMenu;
    }


    private AnAction createSendAction() {
        return new AnAction("发送", "发送", Icons.scaleToWidth(Icons.SEND, 15)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                sendBtn.doClick();
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                super.update(event);
                event.getPresentation().setIcon(sendBtn.getIcon());
                String txt = sendBtn.isCancelable() ? "停止生成" : "发送";
                event.getPresentation().setText(txt);
                event.getPresentation().setDescription(txt);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    private JComponent createInputToolBar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("AskAiActionGroup", false);

        AnAction assistantAction = createSelectAssistantAction();
        AnAction fastCommandAction = createCommandAction();
        AnAction datasetAction = createSelectDatasetAction();
        AnAction sendAction = createSendAction();
        actionGroup.addAction(assistantAction);
        actionGroup.addAction(datasetAction);
        actionGroup.addAction(fastCommandAction);
        actionGroup.addAction(sendAction);

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("AskAiActionToolbar", actionGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());
        // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);

        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);

        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        return actionToolBarComponent;
    }

    private AnAction createSelectDatasetAction() {
        return new AnAction("选择知识库", "选择知识库", Icons.scaleToWidth(Icons.ACTION_DATASET, 15)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Editor editor = input.getEditor();
                    if (editor == null) return;

                    int offset = editor.getSelectionModel().getSelectionEnd();
                    editor.getDocument().insertString(offset, "#");
                    editor.getCaretModel().moveToOffset(offset + 1);
                });
            }
        };
    }

    private AnAction createSelectAssistantAction() {
        return new AnAction("选择应用助手", "选择应用助手", Icons.scaleToWidth(Icons.ACTION_AT, 15)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Editor editor = input.getEditor();
                    if (editor == null) return;

                    int offset = editor.getSelectionModel().getSelectionEnd();
                    editor.getDocument().insertString(offset, "@");
                    editor.getCaretModel().moveToOffset(offset + 1);
                });
            }
        };
    }

    @NotNull
    private ToggleAction createDatasetAction() {
        ToggleAction toggleAction = new ToggleAction("知识库", "使用知识库后, AI问答将结合相关业务领域知识库, 提供更精准的回答！", Icons.DATASET) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return ChatxApplicationSettings.settings().withDataset;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                resetToggleValue();
                resetUploadFiles();
                ChatxApplicationSettings.settings().withDataset = state;
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                super.update(event);
                event.getPresentation().setText(isSelected(event) ? "点击禁用公共知识库" : "点击启用公共知识库");
                event.getPresentation().setDescription("使用知识库后, AI问答将结合相关业务领域知识库, 提供更精准的回答！");
                event.getPresentation().setVisible(Constants.DEFAULT_ASSISTANT.equals(getAssistant()));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
        return toggleAction;
    }

    private CancelableButton createSendBtn() {
        CancelableButton sendBtn = new CancelableButton(Icons.scaleToWidth(Icons.SEND, 15));
        sendBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 10));
        sendBtn.setBorderPainted(false);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setPreferredSize(new Dimension(30, 30));
        sendBtn.setToolTipTxt("发送");
        sendBtn.addActionListener(this::sendButtonClicked);

        sendBtn.setCancelToolTipTxt("停止生成");
        sendBtn.setCancelButton(Icons.scaleToWidth(Icons.TERMINATE2, 16));
        sendBtn.addCancelListener(e -> {
            QuestionPanel questionPanel = project.getService(QuestionPanel.class);
            if (Objects.nonNull(questionPanel)) {
                ChatSwingWorker chatSwingWorker = questionPanel.getChatSwingWorker();
                if (Objects.nonNull(chatSwingWorker)) {
                    chatSwingWorker.cancel(true);
                    askAiDone();
                }
            }
        });

        return sendBtn;
    }

    private JComponent createToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("AiToolbarActionGroup", false);
        actionGroup.addAction(createKitAction());
        actionGroup.addAction(createAiHistoryAction());
        actionGroup.addAction(createIndexAction());
//        actionGroup.addAction(createUploadFileAction());

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("AiToolbarActionToolbar", actionGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());
       // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);

        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        return actionToolBarComponent;
    }

    private AnAction createUploadFileAction() {
        return new AnAction("添加文件进行对话", "添加文件进行对话", Icons.scaleToWidth(Icons.UPLOAD, 16)) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showFileUploadTips(true);
                clearFileUploadTips(true, 5000);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setVisible(Constants.DEFAULT_ASSISTANT.equals(getAssistant()));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    private void showFileUploadTips(boolean showIfExist) {
        int hadCount = tipsCount.get();
        if (!showIfExist && hadCount > 0) return;

//        String tips = "将文件拖拽到对话界面(蓝框内)，即可基于添加的文件进行对话";
//        NotifyUtils.notifyBalloon(ObjectUtils.defaultIfNull(northPanel, component), tips,
//                Icons.UPLOAD, component.getForeground(), EditorComponentUtils.BACKGROUND);

        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        NonOpaquePanel panel = questionPanel.getPanel();
        panel.setBorder(BorderFactory.createLineBorder(JBColor.BLUE));
        tipsCount.incrementAndGet();
    }

    private void clearFileUploadTips(boolean delay, long delayTime) {
        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        JPanel panel = questionPanel.getPanel();
        if (!delay) {
            tipsCount.decrementAndGet();
            panel.setBorder(null);
            return;
        }

        executor.execute(() -> {
            try {
                Thread.sleep(delayTime);
                int getCount = tipsCount.decrementAndGet();
                if (getCount <= 0) {
                    panel.setBorder(null);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private AnAction createAiHistoryAction() {
        return new AnAction("历史会话", "历史会话", Icons.scaleToWidth(Icons.CONVERSATION_HISTORY, 13)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ConversationHistoryDialog.showDialog();
            }
        };
    }

    private AnAction createKitAction() {
        return new AnAction("快捷工具箱", "快捷工具箱", Icons.scaleToWidth(Icons.KIT, 15)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                new KitPopup().show(component, input.getSize());
            }
        };
    }

    private AnAction createIndexAction() {
        return new AnAction("新建会话", "新建会话", Icons.scaleToWidth(Icons.CONVERSATION_NEW, 16)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                QuestionPanel questionPanel = project.getService(QuestionPanel.class);
                if (Objects.nonNull(questionPanel)) questionPanel.repaintPanel();
            }
        };
    }

    private AnAction createCommandAction() {
        return new AnAction("快捷指令", "快捷指令", Icons.scaleToWidth(Icons.ACTION_COMMAND, 15)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Editor editor = input.getEditor();
                    if (editor == null) return;
                    // 光标位置(光标在最后一行末尾时，光标位置为0
                    int offset = editor.getSelectionModel().getSelectionEnd();
                    editor.getDocument().setText("");
                    editor.getDocument().setText("/");
                    editor.getCaretModel().moveToOffset(offset + 1);
                });
            }
        };
    }

    /**
     * 发送按钮点击
     *
     * @param e
     */
    private void sendButtonClicked(ActionEvent e) {
        String question = input.getDocument().getText();
        if (question.isBlank()) {
            askAiDone();
            return;
        }

        // 只有知识库标签时, 不发送
        Pair<String, Set<String>> subDatasetTags = subDatasetTag(question);
        String subDatasetTag = subDatasetTags.getLeft();
        if (StringUtils.isEmpty(subDatasetTag)) {
            askAiDone();
            return;
        }

        startProgress();
        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        question = question.trim();
        // 处理快捷指令
        if (question.startsWith("/")) {
            Map<String, ShortcutInstructionInfo> shortcutMap = ChatxApplicationSettings.settings().shortcutInstructions.stream().collect(Collectors.toMap(ShortcutInstructionInfo::getName, x -> x));
            if (question.endsWith("/")) question = question.substring(0, question.length() - 1);
            ShortcutInstructionInfo shortcut = shortcutMap.get(question.substring(1));
            if (shortcut != null) {
                // clear conversation
                if (StringUtils.isEmpty(shortcut.getPrompt()) && StringUtils.isEmpty(shortcut.getAssistantCode())) {
                    questionPanel.repaintPanel();
                    return;
                }
                // 组装快捷指令
                question = packagePrompt(shortcut, question);
                // 切换助手
                if (StringUtils.isNotBlank(shortcut.getAssistantCode()) && assistantNameLink != null) {
                    flushAssistant(shortcut.getAssistantCode());
                }
            }
        }
        // 回写 datasets
        Set<String> datasets = subDatasetTags.getRight();
        if (CollectionUtils.isNotEmpty(datasets)) {
            setText(String.join("", datasets));
        } else {
            setText("");
        }
        // 发送
        questionPanel.askAi(getAssistant(), QuestionType.ASK, question);
    }

    public void flushAssistant(String assistantCode, boolean init) {
        flushAssistant(assistantCode, null, init);
    }

    public void flushAssistant(String assistantCode) {
        flushAssistant(assistantCode, null);
    }

    public void flushDefaultAssistant() {
        this.getFastCommand().setText(FC_TX);
        this.getFastCommand().setIcon(Icons.scaleToWidth(this.getDefaultCommandIcon(), 13));
        this.flushAssistant(Constants.DEFAULT_ASSISTANT);
    }

    public void flushAssistant(String assistantCode, String newConversationId) {
        flushAssistant(assistantCode, newConversationId, false);
    }

    public void flushAssistant(String assistantCode, String newConversationId, boolean init) {
        // 设置conversationId
        this.conversationId = newConversationId;

        // 切换助手
        if (Objects.isNull(assistantNameLink)) return;
        String oldAssistantCode = getAssistant();
        if (oldAssistantCode.equals(assistantCode) && !init) return;

        AssistantInfo assistant = ChatxApplicationSettings.settings().getAssistantInfo(assistantCode);
        if (StringUtils.isNotEmpty(assistant.getName())) {
            assistantNameLink.setText(assistant.getName());
            // assistantPanel.setVisible(!Constants.DEFAULT_ASSISTANT.equals(assistantCode));
            assistantLabel.setText(String.format(" 和  %s  聊聊 ", assistant.getName()));
            // assistantLabel.setText(String.format("  %s  ", assistant.getName()));

            assistantParam.removeActionListener(resetConversationAction);
            assistantParam.removeAllItems();
            assistantParam.setVisible(false);
            AssistantInfo.Param params = assistant.getCustomParam();
            if (Objects.nonNull(params)) {
                Map<String, String> values = params.getValues();
                if (MapUtils.isNotEmpty(values)) {
                    values.forEach((k, v) -> assistantParam.addItem(new Option<>(k, v)));
                    assistantParam.setVisible(true);
                }
            }
            assistantParam.addActionListener(resetConversationAction);

            if (StringUtils.isNotEmpty(assistant.getIconUrl16())) {
//                Icons.asyncSetUrlIcon(assistantIcon, assistant.getIconUrl16(), defaultAssistantIcon.getIcon(), 16);
            }
            AssistantDetail detail = assistant.getDetail();
            if (Objects.nonNull(detail) && StringUtils.isNotBlank(detail.getOpeningStatement())) {
                QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                uploadFiles.clear();
                repaintNorthContent();
                questionPanel.getAskAiDecorator().hidden();
                // 开场白
                String openingStatement = detail.getOpeningStatement();
                ChatSwingWorker chatSwingWorker = new ChatSwingWorker(ProjectUtils.getCurrProject(), questionPanel, assistantCode, QuestionType.ASK, "", null);
                chatSwingWorker.renderAnswer(openingStatement, createSuggestedQuestions(detail.getSuggestedQuestions()));
                chatSwingWorker.done();
            }
        }
    }

    public JPanel createSuggestedQuestions(List<String> questions) {
        if (CollectionUtils.isEmpty(questions)) return null;

        JPanel suggestedPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        suggestedPanel.setBorder(BorderFactory.createEmptyBorder(-5, 0, 10, 0));

        JLabel q = new JLabel();
        q.setText("试着问我");
        q.setIcon(Icons.scaleToWidth(Icons.STARTS, 15));
        q.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        q.setFont(new Font("微软雅黑", Font.BOLD, 12));
        suggestedPanel.add(q);

        for (String question : questions) {
            JComponent component = StreamMessageBodyFactory.autoSendQuestion(question);
            suggestedPanel.add(component);
        }

        Color background = ColorUtil.PLUGIN_REPLY;
        suggestedPanel.setBackground(background);
        UIUtil.forEachComponentInHierarchy(suggestedPanel, c -> c.setBackground(background));
        return suggestedPanel;
    }

    private ToggleAction createInternetToggleAction() {
        Icon icon = Icons.scaleToWidth(Icons.INTERNET, 20);
        return new ToggleAction("点击开启联网搜索", "点击开启联网搜索", icon) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return StringUtils.defaultString("Y").equals(assistantToggleValue);
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                resetToggleValue();
                resetUploadFiles();
                assistantToggleValue = state ? "Y" : "N";
                conversationId = StringUtils.EMPTY;
                if (state) {
                    NotifyUtils.notifyBalloon(northToolbar, "已接入互联网，Jarvis 会在需要时通过互联网搜集资料",
                            icon, component.getForeground(), EditorComponentUtils.BACKGROUND);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                super.update(event);
                boolean selected = isSelected(event);
                Presentation presentation = event.getPresentation();
                presentation.setText(selected ? "已接入互联网，Jarvis 会在需要时通过互联网搜集资料" : "已关闭 Jarvis 的互联网访问权限");
                presentation.setDescription(selected ? "已接入互联网，Jarvis 会在需要时通过互联网搜集资料" : "已关闭 Jarvis 的互联网访问权限");
                presentation.setVisible(Constants.DEFAULT_ASSISTANT.equals(getAssistant()));
                presentation.setIcon(selected ? Icons.scaleToWidth(Icons.INTERNET, 20) : Icons.scaleToWidth(Icons.NO_INTERNET, 17));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }

    public void enableGitIfIndexed(AnActionEvent e) {
        String status = GitIndexUtils.gitIndexStatus(project).getStatus();
        if (status.equals(GitIndexStatus.done.name()) || status.equals(GitIndexStatus.reindex.name())) {
            gitToggleAction.setSelected(e, true);
        }
    }

    private ToggleAction createGitToggleAction() {
        return new ToggleAction("点击开启代码仓库知识问答", "点击开启代码仓库知识问答", Icons.scaleToWidth(Icons.GIT, 18)) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return StringUtils.defaultString("Y").equals(gitToggleValue);
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                resetToggleValue();
                resetUploadFiles();
                gitToggleValue = state ? "Y" : "N";
                if (state) {
                    NotifyUtils.notifyBalloon(northToolbar, "已开启代码仓库问答，Jarvis 会学习您的代码知识库进行回复",
                            Icons.BUILD_GIT_INDEX, component.getForeground(), EditorComponentUtils.BACKGROUND);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                super.update(event);
                boolean selected = isSelected(event);
                Presentation presentation = event.getPresentation();
                presentation.setText(selected ? "已开启代码仓库问答，Jarvis 会学习您的代码知识库进行回复" : "已关闭 Jarvis 代码仓库问答");
                presentation.setDescription(selected ? "已开启代码仓库问答，Jarvis 会学习您的代码知识库进行回复" : "已关闭 Jarvis 代码仓库问答");
//                presentation.setVisible(Constants.DEFAULT_ASSISTANT.equals(getAssistant()));
                presentation.setIcon(selected ? Icons.scaleToWidth(Icons.GIT, 18) : Icons.scaleToWidth(Icons.NO_GIT, 18));

                Project project = event.getProject();
                if (Objects.isNull(project)) return;

                // gitInfo
                QuestionPanel questionPanel = project.getService(QuestionPanel.class);
                JLabel gitInfoLabel = questionPanel.getInputPanelFactory().getGitInfo();
                String gitInfo = StringUtils.defaultIfEmpty(GitUtils.getBranchName(project), GitUtils.getCurrentSimpleRevision(project));

                if (StringUtils.isNotBlank(gitInfo)) {
                    String status = GitIndexUtils.gitIndexStatus(project).getStatus();
                    gitInfoLabel.setIcon(null);
                    if (GitIndexStatus.done.name().equals(status)) {
                        gitInfoLabel.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;索引完成</html>", gitInfo));
                        gitInfoLabel.setToolTipText("开启仓库问答试试吧");
                    } else if (GitIndexStatus.failed.name().equals(status)) {
                        gitInfoLabel.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;索引失败</html>", gitInfo));
                        gitInfoLabel.setIcon(buildIndexFailIcon);
                        gitInfoLabel.setToolTipText(GitIndexUtils.gitIndexStatus(project).getReason());
                    } else if (GitIndexStatus.init.name().equals(status)) {
                        gitInfoLabel.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;索引中</html>", gitInfo));
                        // gitInfoLabel.setIcon(Icons.LOADING_ANIMATED);
                    } else if (GitIndexStatus.reindex.name().equals(status)) {
                        gitInfoLabel.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;索引更新中</html>", gitInfo));
                        // gitInfoLabel.setIcon(Icons.LOADING_ANIMATED);
                    } else {
                        gitInfoLabel.setText(String.format("<html>分支&nbsp;<u style=\"color: rgb(88,157,246);\">%s</u>&nbsp;未索引</html>", gitInfo));
                    }
                    gitInfoLabel.setVisible(true);
                    String key = GitIndexUtils.indexKey(project);
                    if (!key.equals(lastGitIndexKey)) {
                        // 初始化或者切分支操作
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            GitIndexUtils.flushGitIndexStatus(project);
                            lastGitIndexKey = key;
                        });
                    }
                } else {
                    gitInfoLabel.setVisible(false);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
    }


    private JComponent createNorthToolbar(AnAction... actions) {
        DefaultActionGroup actionGroup = new DefaultActionGroup("AiNorthToolbarActionGroup", false);
        for (AnAction action : actions) {
            actionGroup.addAction(action);
        }

        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("AiNorthToolbar", actionGroup, true);
        actionToolBar.setTargetComponent(actionToolBar.getComponent());
        // actionToolBar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
        actionToolBar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);

        JComponent actionToolBarComponent = actionToolBar.getComponent();
        actionToolBarComponent.setBorder(null);
        actionToolBarComponent.setOpaque(false);
        return actionToolBarComponent;
    }

    private Pair<String, Set<String>> subDatasetTag(String question) {
        Set<String> datasets = new HashSet<>();
        if (question.contains("#")) {
            List<DatasetInfo> datasetList = ChatxApplicationSettings.settings().datasets.stream().toList();

            // #dataset
            for (DatasetInfo dataset : datasetList) {
                String datasetName = "#" + dataset.getDatasetName();
                if (question.contains(datasetName)) {
                    datasets.add((datasets.size() % 2 == 0 && !datasets.isEmpty() ? datasetName + " \n" : datasetName + " "));
                    question = StringUtils.substringBefore(question, datasetName) + StringUtils.substringAfter(question, datasetName);
                }
            }
            // #all
            if (question.contains(Constants.ALL_DATASET)) {
                question = question.replace(Constants.ALL_DATASET, "");
                datasets.clear();
                datasets.add(Constants.ALL_DATASET + " ");
            }

            return Pair.of(question.trim(), datasets);
        }
        return Pair.of(question, new HashSet<>());
    }

    public String packagePrompt(ShortcutInstructionInfo shortcut, String question) {
        if (shortcut != null) {
            if (!shortcut.isPackagePrompt()) return question;

            // 执行快捷指令
            String gitRepoUrl = GitUtils.getCurrentProjectGitUrl();
            String branchName = GitUtils.getCurrentProjectBranchName();
            String moduleName = Optional.ofNullable(EditorUtil.getSelectedEditor(ProjectUtils.getCurrProject())).map(EditorUtil::getPsiFile).map(ModuleUtil::findModuleForFile).map(Module::getName).orElse("");
            String prompt = shortcut.getPrompt();
            prompt = prompt.replaceAll("\\{\\{module\\}\\}", Matcher.quoteReplacement(moduleName));
            prompt = prompt.replaceAll("\\{\\{gitRepo\\}\\}", Matcher.quoteReplacement(gitRepoUrl));
            prompt = prompt.replaceAll("\\{\\{gitBranch\\}\\}", Matcher.quoteReplacement(branchName));
            question = prompt;
        }
        return question;
    }

    public void askAiDone() {
        if (Objects.nonNull(sendBtn)) sendBtn.done();
        stopProgress();


        QuestionPanel questionPanel = project.getService(QuestionPanel.class);
        if (Objects.nonNull(questionPanel)) {
            JPanel panel = questionPanel.getViewPanel();
            UIUtil.forEachComponentInHierarchy(panel, this::supportDragAndDrop);
        }
    }

    public void setText(String text) {
        Editor editor = input.getEditor();
        WriteCommandAction.writeCommandAction(project).run(() -> {
            editor.getDocument().replaceString(0, editor.getDocument().getTextLength(), text);
        });
    }

    @Override
    public void dispose() {
        if (Objects.nonNull(scheduledFuture)) {
            scheduledFuture.cancel(true);
        }
        scheduledFuture = null;
    }

    class SendAction extends DumbAwareAction {

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (sendBtn.isFree()) {
                sendBtn.doClick();
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(CompletionServiceImpl.getCurrentCompletionProgressIndicator() == null);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    class MyTextFieldCompletionProvider extends TextFieldCompletionProvider {
        @Override
        public @Nullable String getPrefix(@NotNull String text, int offset) {
            String prefix = text.substring(Math.max(0, offset - 1), offset);
            if (prefix.equals("#") || prefix.equals("/") || prefix.equals("@")) {
                return prefix;
            }
            return super.getPrefix(text, offset);
        }

        @Override
        protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix, @NotNull CompletionResultSet result) {
            String assistant = getAssistant();

            // shortcut instruct
            List<ShortcutInstructionInfo> instructions = ChatxApplicationSettings.settings().getInputInstructions(assistant);
            for (ShortcutInstructionInfo shortcutInstruction : instructions) {
                result.addElement(LookupElementBuilder.create("/" + shortcutInstruction.getName()).withPresentableText("/" + shortcutInstruction.getName()).withTypeText(shortcutInstruction.getDesc()).bold().withIcon(instructionsIcons.getOrDefault(shortcutInstruction.getName(), defaultInstructionsIcon).getIcon()));
            }

            // dataset
//            result.addElement(LookupElementBuilder.create("#ALL" + " ")
//                    .withPresentableText("#ALL" + "                                              ")
//                    .withTypeText("所有知识库").bold()
//                    .withIcon(Icons.scaleToWidth(Icons.DATASET, 13))
//            );
            List<DatasetInfo> datasets = ChatxApplicationSettings.settings().getDatasets(assistant);
            for (DatasetInfo dataset : datasets) {
                result.addElement(LookupElementBuilder.create("#" + dataset.getDatasetName() + " ").withPresentableText("#" + dataset.getDatasetName()).withTypeText(StringUtils.defaultString(dataset.getDatasetDesc(), "知识库")).bold().withIcon(datasetsIcons.getOrDefault(dataset.getDatasetId(), defaultDatasetsIcon).getIcon()));
            }
            if (CollectionUtils.isEmpty(datasets)) {
                result.addElement(LookupElementBuilder.create("#_NOTHING_DATASET").withPresentableText("#暂无知识库").withTypeText("当前助手未绑定知识库").bold().withIcon(defaultDatasetsIcon.getIcon()));
            }

            // assistant
//            AssistantInfo defaultAssistant = ChatxApplicationSettings.settings().getAssistantInfo(Constants.DEFAULT_ASSISTANT);
//            if (Objects.nonNull(defaultAssistant)) {
//                if (StringUtils.isNotEmpty(dropDownLink.getText()) && !dropDownLink.getText().equals(defaultAssistant.getName())) {
//                    result.addElement(LookupElementBuilder.create("@不使用助手")
//                                    .withPresentableText("@不使用助手" + "                                              ")
//                                    .withTypeText("关闭当前助手").bold()
//                            // .withIcon(assistantIcons.getOrDefault(defaultAssistant.getCode(), defaultAssistantIcon).getIcon())
//                    );
//                }
//            }

            List<AssistantInfo> assistants = ChatxApplicationSettings.settings().assistants;
            for (AssistantInfo assistantInfo : assistants) {
                // if (Constants.DEFAULT_ASSISTANT.equals(assistant.getCode())) continue;
                result.addElement(LookupElementBuilder.create("@" + assistantInfo.getName()).withPresentableText("@" + assistantInfo.getName()).withTypeText(StringUtils.defaultString(assistantInfo.getDesc(), "助手")).bold().withIcon(assistantIcons.getOrDefault(assistantInfo.getCode(), defaultAssistantIcon).getIcon()));
            }
        }
    }

    private static final Map<String, JLabel> assistantIcons = new HashMap<>();
    private static final JLabel defaultAssistantIcon = new JLabel(Icons.scaleToWidth(Icons.ASSISTANT, 13));
    private static final Map<String, JLabel> datasetsIcons = new HashMap<>();
    private static final JLabel defaultDatasetsIcon = new JLabel(Icons.scaleToWidth(Icons.DATASET, 13));
    private static final Map<String, JLabel> instructionsIcons = new HashMap<>();
    private static final JLabel defaultInstructionsIcon = new JLabel(Icons.scaleToWidth(Icons.AGENT_TASK, 13));

//    public void initResource(List<AssistantInfo> assistants, List<DatasetInfo> datasets, List<ShortcutInstructionInfo> instructions) {
//        for (AssistantInfo assistant : assistants) {
//            JLabel label = new JLabel(assistant.getName());
//            Icons.asyncSetUrlIcon(label, assistant.getIconUrl16(), Icons.ASSISTANT, 13);
//            assistantIcons.put(assistant.getCode(), label);
//        }
//        for (DatasetInfo dataset : datasets) {
//            JLabel label = new JLabel(dataset.getDatasetName());
//            Icons.asyncSetUrlIcon(label, dataset.getIconUrl16(), Icons.DATASET, 13);
//            datasetsIcons.put(dataset.getDatasetId(), label);
//        }
//        for (ShortcutInstructionInfo instruction : instructions) {
//            JLabel label = new JLabel(instruction.getName());
//            Icons.asyncSetUrlIcon(label, instruction.getIconUrl16(), Icons.ASSISTANT, 13);
//            instructionsIcons.put(instruction.getName(), label);
//        }
//    }
}
