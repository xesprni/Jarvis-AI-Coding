package com.qihoo.finance.lowcode.declarative.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DDLInfo;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.declarative.action.DeclarativeSQLAction;
import com.qihoo.finance.lowcode.declarative.entity.DiffTableNode;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * DeclareDiffPanel
 *
 * @author fengjinfu-jk
 * date 2024/4/26
 * @version 1.0.0
 * @apiNote DeclareDiffPanel
 */
@Data
public class DeclareDiffPanel {
    private final JPanel mainPanel;
    private final List<JBCheckBox> ddlCheckBoxList = new ArrayList<>();

    private boolean frozen;
    private DiffTableNode tableNode;
    private JPanel checkBoxPanel;
    private Consumer<List<String>> selectedDDLs;
    private JComponent northPanel;

    private static final JPanel emptyPanel = new JPanel(new BorderLayout());

    static {
        JLabel none = new JLabel("暂无数据");
        none.setForeground(JBColor.GRAY);
        none.setHorizontalAlignment(JLabel.CENTER);
        none.setBorder(BorderFactory.createEmptyBorder(20, 0, 100, 0));

        JLabel noneRecord = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 260));
        noneRecord.setBorder(BorderFactory.createEmptyBorder(150, 0, 0, 0));

        emptyPanel.add(noneRecord, BorderLayout.CENTER);
        emptyPanel.add(none, BorderLayout.SOUTH);
    }

    public List<String> getSelectedDDLs() {
        return ddlCheckBoxList.stream().filter(JBCheckBox::isSelected).map(JBCheckBox::getText).collect(Collectors.toList());
    }

    public DeclareDiffPanel() {
        this.mainPanel = new JPanel(new BorderLayout());
    }

    public void repaint() {
        if (frozen) {
            // JComponent component = CollectionUtils.isNotEmpty(ddlCheckBoxList) ? ddlCheckBoxList.get(0) : mainPanel;
            // NotifyUtils.notifyBalloon(component, "任务执行中, 窗口冻结...", MessageType.WARNING, true);
            return;
        }

        this.mainPanel.removeAll();
        Pair<Component, Component> componentPair = diffPanel(tableNode);
        // Component toolbar = componentPair.getLeft();
        Component diffContent = componentPair.getRight();
        JPanel diffPanel = new JPanel(new BorderLayout());
        // diffPanel.add(toolbar);
        if (Objects.nonNull(northPanel)) {
            diffPanel.add(northPanel, BorderLayout.NORTH);
        }
        diffPanel.add(diffContent, BorderLayout.CENTER);

        this.mainPanel.add(diffPanel, BorderLayout.CENTER);
        JComponent diffDDLPanel = diffDDLPanel(tableNode);
        if (Objects.nonNull(diffDDLPanel)) {
            this.mainPanel.add(diffDDLPanel, BorderLayout.SOUTH);
        }
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
        ddlSelectedAction();
    }

    public void setNorthPanel(@NotNull JComponent component) {
        this.northPanel = component;
    }

    private Pair<Component, Component> diffPanel(DiffTableNode tableNode) {
        if (Objects.isNull(tableNode)) return Pair.of(new JPanel(), emptyPanel);

        Project project = ProjectUtils.getCurrProject();
        // 创建右侧提供的文本的DiffContent
        LightVirtualFile sqlLight = new LightVirtualFile(LightVirtualType.SQL.getValue());
        DiffContent leftContent = DiffContentFactory.getInstance()
                .create(project, StringUtils.defaultString(tableNode.getDbDDL()), sqlLight);

        // 创建右侧提供的文本的DiffContent
        DiffContent rightContent = DiffContentFactory.getInstance()
                .create(project, StringUtils.defaultString(tableNode.getDeclareDDL()), sqlLight);
        CacheDiffRequestChainProcessor processor = createChainProcessor(tableNode, leftContent, rightContent);
        if (processor.getComponent().getComponent(0) instanceof JPanel panel) {
            return Pair.of(panel.getComponent(0), panel.getComponent(1));
        }
        return Pair.of(new JPanel(), processor.getComponent());
    }

    @NotNull
    private static CacheDiffRequestChainProcessor createChainProcessor(DiffTableNode tableNode, DiffContent leftContent, DiffContent rightContent) {
        DeclarativeSQLAction action = (DeclarativeSQLAction) ActionManager.getInstance().getAction(DeclarativeSQLAction.ACTION_ID);
        String path = action.getDeclarativeSQLFilePath();

        SimpleDiffRequest diffRequest = new SimpleDiffRequest("DDL差异", leftContent, rightContent,
                String.format("%s.%s", tableNode.getDatabase().getActualDatabase(), tableNode.getTableName()),
                String.format("【%s/%s.sql】%s", path, tableNode.getDatabaseName(), tableNode.getTableName())
        );
        DiffRequestChain requestChain = new SimpleDiffRequestChain(diffRequest);
        DeclareDiffWindow declareDiffWindow = new DeclareDiffWindow(ProjectUtils.getCurrProject(), requestChain, DiffDialogHints.DEFAULT);
        declareDiffWindow.init();

        // DiffRequestProcessor#myMainPanel
        CacheDiffRequestChainProcessor processor = (CacheDiffRequestChainProcessor) declareDiffWindow.getProcessor();
        processor.updateRequest();
        return processor;
    }

    private JComponent diffDDLPanel(DiffTableNode tableNode) {
        ddlCheckBoxList.clear();
        if (Objects.isNull(tableNode)) return null;

        JBScrollPane scrollPane = new JBScrollPane();
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 0, 0, 0);
        Border lineBorder = BorderFactory.createLineBorder(JBColor.border());
        scrollPane.setBorder(BorderFactory.createCompoundBorder(emptyBorder, lineBorder));
        scrollPane.setPreferredSize(new Dimension(-1, 220));

        List<DDLInfo> ddlInfos = ListUtils.defaultIfNull(tableNode.getDiffDDLs(), new ArrayList<>());
        if (CollectionUtils.isEmpty(ddlInfos)) return scrollPane;

        checkBoxPanel = new JPanel(new VerticalFlowLayout());
        for (DDLInfo diffDDL : ddlInfos) {
            String sql = diffDDL.getSql();
            JBCheckBox checkBox = new JBCheckBox(sql);
            if (diffDDL.isDrop()) {
                checkBox.setForeground(JBColor.RED);
            }

            checkBox.setToolTipText(sql);
            checkBox.addActionListener(e -> ddlSelectedAction());
            checkBox.setSelected(true);
            ddlCheckBoxList.add(checkBox);
            checkBoxPanel.add(checkBox);
        }

        scrollPane.setViewportView(checkBoxPanel);
        return scrollPane;
    }

    private void ddlSelectedAction() {
        if (Objects.nonNull(selectedDDLs)) {
            selectedDDLs.accept(getSelectedDDLs());
        }
    }

    public void frozen() {
        frozen = true;
        for (JBCheckBox checkBox : ddlCheckBoxList) {
            checkBox.setEnabled(false);
        }
    }

    public void thaw() {
        frozen = false;
    }
}
