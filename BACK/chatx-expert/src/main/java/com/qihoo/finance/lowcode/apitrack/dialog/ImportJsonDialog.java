package com.qihoo.finance.lowcode.apitrack.dialog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.apitrack.dialog.table.JsonTableWrap;
import com.qihoo.finance.lowcode.apitrack.entity.JsonFormNode;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.ui.base.EditorSettingsInit;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ImportJsonDialog
 *
 * @author fengjinfu-jk
 * date 2023/10/8
 * @version 1.0.0
 * @apiNote ImportJsonDialog
 */
public class ImportJsonDialog extends DialogWrapper {
    @Getter
    private JPanel mainPanel;
    private EditorImpl editor;
    private final Project project;

    private final JsonTableWrap tableWrap;
    private final JTable jsonTable;

    protected ImportJsonDialog(@Nullable Project project, JsonTableWrap tableWrap, JTable jsonTable) {
        super(project);
        this.project = project;
        this.tableWrap = tableWrap;
        this.jsonTable = jsonTable;

        initEditor();
        this.init();
        setTitle(GlobalDict.TITLE_INFO + "-Json导入");
        setOKButtonText("导入");
        setCancelButtonText("取消");

    }

    @Override
    protected Action @NotNull [] createActions() {
        // 创建自定义按钮的Action
        Action customAction = new AbstractAction("格式化") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String json = editor.getDocument().getText();
                if (StringUtils.isEmpty(json)) return;

                try {
                    Object object = JSON.parseObject(json, Object.class);

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        String jsonByFormat = JSON.toJSONString(object, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);

                        editor.getDocument().setText(jsonByFormat);
                        editor.getMarkupModel().getAllHighlighters(); // 刷新高亮显示
                        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE); // 滚动到光标位置
                        editor.repaint(0, editor.getDocument().getText().length()); // 重绘编辑器
                    });
                } catch (Exception ex) {
                    // 错误的JSON文本
                    Messages.showMessageDialog("请输入正确格式的JSON文本", "JSON格式化", Icons.scaleToWidth(Icons.FAIL, 60));
                }
            }
        };

        // 将自定义按钮的Action添加到对话框中
        return new Action[]{customAction, getOKAction(), getCancelAction()};
    }

    public void initEditor() {
        this.mainPanel = new JPanel(new BorderLayout());
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        document.setReadOnly(false);
        this.editor = (EditorImpl) editorFactory.createEditor(document);
        // 初始默认设置
        EditorSettingsInit.init(this.editor);
        // 添加监控事件
        this.editor.getDocument().setReadOnly(false);


        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        this.editor.setHighlighter(highlighterFactory.createEditorHighlighter(project, new LightVirtualFile("import.json")));

        // 初始化面板
        this.mainPanel.add(editor.getComponent(), BorderLayout.CENTER);
        this.mainPanel.setPreferredSize(JBUI.size(1000, 500));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.mainPanel;
    }

    @Override
    protected void doOKAction() {
        String json = this.editor.getDocument().getText();
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(json);
        } catch (Exception e) {
            // 错误的JSON文本
            Messages.showMessageDialog("请输入正确格式的JSON文本", "JSON导入", Icons.scaleToWidth(Icons.FAIL, 60));
            return;
        }

        Map<String, JsonFormNode> jsonNode = convent(jsonObject);

        JsonFormNode root = new JsonFormNode();
        root.setType("object");
        root.setProperties(jsonNode);

        String jsonRaw = JSON.toJSONString(root);
        this.tableWrap.setStructureJson(jsonRaw);
        Object[][] editTableData = this.tableWrap.getEditTableData();

        DefaultTableModel model = (DefaultTableModel) this.jsonTable.getModel();
        int rowCount = model.getRowCount();
        for (Object[] editTableDatum : editTableData) {
            model.addRow(editTableDatum);
            JsonTableWrap.adjustColumnWidth(jsonTable, model.getRowCount() - 1);
        }

        for (int i = 0; i < rowCount; i++) {
            model.removeRow(0);
        }

        super.doOKAction();
    }

    private Map<String, JsonFormNode> convent(JSONObject jsonObject) {
        Map<String, JsonFormNode> nodeMap = new HashMap<>();
        if (Objects.isNull(jsonObject)) return nodeMap;

        jsonObject.forEach((key, val) -> {
            JsonFormNode node = new JsonFormNode();
            nodeMap.put(key, node);
            node.setName(key);

            if (val instanceof JSONObject) {
                node.setType("object");
                node.setProperties(convent((JSONObject) val));
            }
            if (val instanceof JSONArray) {
                JsonFormNode items = buildArrayItems(val);
                node.setItems(items);
                node.setType("array");
            }
            if (val instanceof String) {
                node.setType("string");
            }
            if (val instanceof Integer) {
                node.setType("integer");
            }
            if (val instanceof Number) {
                node.setType("number");
            }
            if (val instanceof Boolean) {
                node.setType("boolean");
            }
        });

        return nodeMap;
    }

    private JsonFormNode buildArrayItems(Object val) {
        JsonFormNode items = new JsonFormNode();
        items.setName("items");

        JSONArray arrayVal = ((JSONArray) val);
        Object itemsObj = new JSONObject();
        if (!arrayVal.isEmpty()) {
            itemsObj = arrayVal.get(0);
        }
        if (itemsObj instanceof JSONObject) {
            if (!arrayVal.isEmpty()) {
                JSONObject arrayObj = arrayVal.getJSONObject(0);
                items.setProperties(convent(arrayObj));
            }
            items.setType("object");
        } else if (itemsObj instanceof JSONArray) {
            JsonFormNode childItems = buildArrayItems(itemsObj);
            items.setItems(childItems);
            items.setType("array");
        } else if (itemsObj instanceof String) {
            items.setType("string");
        } else if (itemsObj instanceof Integer) {
            items.setType("integer");
        } else if (itemsObj instanceof Number) {
            items.setType("number");
        } else if (itemsObj instanceof Boolean) {
            items.setType("boolean");
        }

        return items;
    }
}
