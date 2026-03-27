package com.qihoo.finance.lowcode.gentracker.ui.component;

import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.components.JBLabel;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.service.MySQLGenerateService;
import com.qihoo.finance.lowcode.gentracker.service.TableInfoSettingsService;
import com.qihoo.finance.lowcode.gentracker.tool.CollectionUtil;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.ui.base.EditorSettingsInit;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 实时调试组件
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class RealtimeDebugComponent {
    @Getter
    private final JPanel mainPanel;

    private ComboBox<String> comboBox;

    /**
     * 所有表
     */
    private Map<String, MySQLTableNode> allTables;

    private final EditorComponent<Template> editorComponent;

    public RealtimeDebugComponent(EditorComponent<Template> editorComponent) {
        this.editorComponent = editorComponent;
        this.mainPanel = new JPanel(new FlowLayout());
        this.init();
        this.refreshTable();
    }

    private void init() {
        this.mainPanel.add(new JBLabel("<html>Debug 实时调试 <span style=\"color: red;\">（仅作调试, IDEA重启后自动重置）</span></html>"));
        // 支持搜索的下拉框
        this.comboBox = new ComboBox<>();
        // 开启搜索支持
        this.comboBox.setSwingPopup(false);
        this.mainPanel.add(this.comboBox);
        // 提交按钮
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction(AllIcons.Debugger.Console) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runDebug();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                String selectVal = (String) comboBox.getSelectedItem();
                e.getPresentation().setEnabled(allTables != null && allTables.containsKey(selectVal));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Template Debug", actionGroup, true);

        actionToolbar.setTargetComponent(actionToolbar.getComponent());
        this.mainPanel.add(actionToolbar.getComponent());
    }

    private void runDebug() {
        // 获取选中的表
        String name = (String) comboBox.getSelectedItem();
        MySQLTableNode dbTable = this.allTables.get(name);
        if (dbTable == null) {
            return;
        }
        // 获取表信息
        TableInfo tableInfo = TableInfoSettingsService.getInstance().getTableInfo(dbTable);
        // 为未配置的表设置一个默认包名
        if (tableInfo.getSavePackageName() == null) {
            tableInfo.setSavePackageName("com.companyname.modulename");
        }
        // 生成代码
        // generate batchNo
        String batchNo = UUID.randomUUID().toString().replaceAll("-", "");
        String code = MySQLGenerateService.getInstance(ProjectUtils.getCurrProject()).debugGenerate(new Template(null, false, false, false, "temp", editorComponent.getFile().getCode(), "0"), tableInfo, batchNo);
        String fileName = editorComponent.getFile().getName();
        // 创建编辑框
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(code);
        // 标识为模板，让velocity跳过语法校验
        document.putUserData(FileTemplateManager.DEFAULT_TEMPLATE_PROPERTIES, FileTemplateManager.getInstance(ProjectUtils.getCurrProject()).getDefaultProperties());
        Editor editor = editorFactory.createViewer(document, ProjectUtils.getCurrProject());
        // 配置编辑框
        EditorSettingsInit.init(editor);
        ((EditorEx) editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(ProjectUtils.getCurrProject(), fileName));
        // 构建dialog
        DialogBuilder dialogBuilder = new DialogBuilder(ProjectUtils.getCurrProject());
        dialogBuilder.setTitle(GlobalDict.TITLE_INFO);
        JComponent component = editor.getComponent();
        component.setPreferredSize(new Dimension(800, 600));
        dialogBuilder.setCenterPanel(component);
        dialogBuilder.addCloseButton();
        dialogBuilder.addDisposable(() -> {
            //释放掉编辑框
            editorFactory.releaseEditor(editor);
            dialogBuilder.dispose();
        });
        dialogBuilder.show();
    }

    private void refreshTable() {
        this.allTables = new HashMap<>(16) {{
            MySQLTableNode table = new MySQLTableNode();
            table.setTableName("test");
            table.setTableComment("测试");
            table.setEngine("InnoDB");
            table.setCharset("utf8mb4");

            List<DatabaseColumnNode> columnNodes = new ArrayList<>();

            DatabaseColumnNode id = new DatabaseColumnNode();
            id.setPK(true);
            id.setNotNull(true);
            id.setFieldName("id");
            id.setFieldComment("id");
            id.setFieldLength(16);
            id.setFieldPrecision(0);
            id.setFieldType("bigint");
            columnNodes.add(id);

            DatabaseColumnNode deleted_at = new DatabaseColumnNode();
            deleted_at.setPK(false);
            deleted_at.setNotNull(true);
            deleted_at.setFieldName("deleted_at");
            deleted_at.setFieldComment("deleted_at");
            deleted_at.setFieldLength(16);
            deleted_at.setFieldPrecision(0);
            deleted_at.setFieldType("bigint");
            columnNodes.add(deleted_at);

            DatabaseColumnNode date_created = new DatabaseColumnNode();
            date_created.setPK(false);
            date_created.setNotNull(true);
            date_created.setFieldName("date_created");
            date_created.setFieldComment("date_created");
            date_created.setFieldLength(null);
            date_created.setFieldPrecision(null);
            date_created.setFieldType("datetime");
            columnNodes.add(date_created);

            DatabaseColumnNode created_by = new DatabaseColumnNode();
            created_by.setPK(false);
            created_by.setNotNull(true);
            created_by.setFieldName("created_by");
            created_by.setFieldComment("created_by");
            created_by.setFieldLength(32);
            created_by.setFieldPrecision(0);
            created_by.setFieldType("varchar");
            columnNodes.add(created_by);

            DatabaseColumnNode date_updated = new DatabaseColumnNode();
            date_updated.setPK(false);
            date_updated.setNotNull(true);
            date_updated.setFieldName("date_updated");
            date_updated.setFieldComment("date_updated");
            date_updated.setFieldLength(null);
            date_updated.setFieldPrecision(null);
            date_updated.setFieldType("datetime");
            columnNodes.add(date_updated);

            DatabaseColumnNode updated_by = new DatabaseColumnNode();
            updated_by.setPK(false);
            updated_by.setNotNull(true);
            updated_by.setFieldName("updated_by");
            updated_by.setFieldComment("updated_by");
            updated_by.setFieldLength(32);
            updated_by.setFieldPrecision(0);
            updated_by.setFieldType("varchar");
            columnNodes.add(updated_by);

            DatabaseColumnNode name = new DatabaseColumnNode();
            name.setPK(false);
            name.setNotNull(true);
            name.setFieldName("name");
            name.setFieldComment("name");
            name.setFieldLength(32);
            name.setFieldPrecision(0);
            name.setFieldType("varchar");
            columnNodes.add(name);

            DatabaseColumnNode code = new DatabaseColumnNode();
            code.setPK(false);
            code.setNotNull(true);
            code.setFieldName("code");
            code.setFieldComment("code");
            code.setFieldLength(32);
            code.setFieldPrecision(0);
            code.setFieldType("varchar");
            columnNodes.add(code);

            columnNodes.forEach(table::add);
            put("test", table);
        }};

        this.comboBox.removeAllItems();
        for (String tableName : getAllTableNameBySort()) {
            this.comboBox.addItem(tableName);
        }
    }


    private List<String> getAllTableNameBySort() {
        if (CollectionUtil.isEmpty(this.allTables)) {
            return Collections.emptyList();
        }
        // 表排前面，视图排后面
        List<String> tableList = new ArrayList<>();
        List<String> viewList = new ArrayList<>();
        for (String name : this.allTables.keySet()) {
            if (name.endsWith("table")) {
                tableList.add(name);
            } else {
                viewList.add(name);
            }
        }
        // 排序后进行拼接
        Collections.sort(tableList);
        Collections.sort(viewList);
        tableList.addAll(viewList);
        return tableList;
    }
}
