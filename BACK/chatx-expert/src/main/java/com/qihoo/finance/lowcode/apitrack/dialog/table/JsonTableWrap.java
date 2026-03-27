package com.qihoo.finance.lowcode.apitrack.dialog.table;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.entity.JsonFormNode;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TableSetting
 *
 * @author fengjinfu-jk
 * date 2023/9/4
 * @version 1.0.0
 * @apiNote TableSetting
 */
@Slf4j
public class JsonTableWrap implements BaseJTableWrap {
    @Setter
    private String structureJson;
    private final boolean isEdit;
    private static final int jsonColumn = 0;
    private static final int jsonFieldTypeColumn = 2;
    private static final String ARRAY = "array";
    private static final String ITEMS = "items";
    private static final String OBJECT = "object";
    private static final Dimension rowHigh = new Dimension(-1, 26);

    public JsonTableWrap(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public static String getRawJson(JTable table) {
        JsonFormNode rootNode = getRootNode(table);
        return JSON.toJson(rootNode);
    }

    private static JsonFormNode getRootNode(JTable table) {
        List<JsonFormNode> jsonFormNodes = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            JsonFormNode node = (JsonFormNode) table.getValueAt(row, 0);
            fillNode(node, table, row);
            jsonFormNodes.add(node);
        }

        JsonFormNode rootNode = new JsonFormNode();
        rootNode.setType(OBJECT);

        // rename
        for (JsonFormNode node : jsonFormNodes) {
            refreshPropertiesNode(node);
        }

        // properties
        List<JsonFormNode> rootChildren = jsonFormNodes.stream().filter(n -> n.getLevel() == 0).collect(Collectors.toList());
        Map<String, List<JsonFormNode>> validateDuplicate = rootChildren.stream().collect(Collectors.groupingBy(JsonFormNode::getName));
        List<String> duplicateKey = new ArrayList<>();
        for (Map.Entry<String, List<JsonFormNode>> entry : validateDuplicate.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateKey.add(entry.getKey());
            }
        }
        if (CollectionUtils.isNotEmpty(duplicateKey)) {
            String duplicateKeyStr = String.format("存在同名字段: %s", String.join(", ", duplicateKey));
            ApiDesignDialog.validateSuccess.set(Pair.of(false, duplicateKeyStr));
            return rootNode;
        }

        Map<String, JsonFormNode> rootProperties = rootChildren.stream().collect(Collectors.toMap(JsonFormNode::getName, Function.identity()));
        rootNode.getProperties().putAll(rootProperties);

        // items

        // required
        List<String> required = rootChildren.stream().filter(JsonFormNode::isCurrentRequired).map(JsonFormNode::getName).collect(Collectors.toList());
        rootNode.getRequired().addAll(required);

        return rootNode;
    }

    private static void fillNode(JsonFormNode node, JTable table, int row) {
        // "参数", "必须", "类型", "mock", "中文名称", "备注", "操作"
        Object require = table.getValueAt(row, 1);
        node.setCurrentRequired(Objects.nonNull(require) ? (Boolean) require : false);

        Object fieldType = table.getValueAt(row, 2);
        node.setType(Objects.nonNull(fieldType) ? fieldType.toString() : "string");

        Object mock = table.getValueAt(row, 3);
        node.setMock(Objects.nonNull(mock) ? mock.toString() : null);

        Object title = table.getValueAt(row, 4);
        node.setTitle(Objects.nonNull(title) ? title.toString() : StringUtils.EMPTY);

        Object description = table.getValueAt(row, 5);
        node.setDescription(Objects.nonNull(description) ? description.toString() : StringUtils.EMPTY);
    }

    @Override
    public boolean isEdit() {
        return isEdit;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"参数", "必须", "类型", "mock", "中文名称", "备注", "操作"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        return new Object[0][];
    }

    @Override
    public Object[][] getEditTableData() {
//        String json = "{\n" + "\t\"type\": \"object\",\n" + "\t\"properties\": {\n" + "\t\t\"name\": {\n" + "\t\t\t\"type\": \"string\",\n" + "\t\t\t\"title\": \"姓名\",\n" + "\t\t\t\"description\": \"用户的姓名\"\n" + "\t\t},\n" + "\t\t\"age\": {\n" + "\t\t\t\"type\": \"integer\",\n" + "\t\t\t\"description\": \"用户的年龄\"\n" + "\t\t},\n" + "\t\t\"data\": {\n" + "\t\t\t\"type\": \"object\",\n" + "\t\t\t\"properties\": {\n" + "\t\t\t\t\"desc\": {\n" + "\t\t\t\t\t\"type\": \"string\"\n" + "\t\t\t\t},\n" + "\t\t\t\t\"innerData\": {\n" + "\t\t\t\t\t\"type\": \"array\",\n" + "\t\t\t\t\t\"items\": {\n" + "\t\t\t\t\t\t\"type\": \"object\",\n" + "\t\t\t\t\t\t\"properties\": {\n" + "\t\t\t\t\t\t\t\"titile\": {\n" + "\t\t\t\t\t\t\t\t\"type\": \"string\"\n" + "\t\t\t\t\t\t\t},\n" + "\t\t\t\t\t\t\t\"code\": {\n" + "\t\t\t\t\t\t\t\t\"type\": \"integer\",\n" + "\t\t\t\t\t\t\t\t\"title\": \"编码\"\n" + "\t\t\t\t\t\t\t}\n" + "\t\t\t\t\t\t},\n" + "\t\t\t\t\t\t\"required\": [\n" + "\t\t\t\t\t\t\t\"titile\",\n" + "\t\t\t\t\t\t\t\"code\"\n" + "\t\t\t\t\t\t]\n" + "\t\t\t\t\t}\n" + "\t\t\t\t}\n" + "\t\t\t},\n" + "\t\t\t\"required\": [\n" + "\t\t\t\t\"desc\",\n" + "\t\t\t\t\"innerData\"\n" + "\t\t\t]\n" + "\t\t}\n" + "\t},\n" + "\t\"required\": [\n" + "\t\t\"name\",\n" + "\t\t\"age\",\n" + "\t\t\"data\"\n" + "\t]\n" + "}";
        this.structureJson = StringUtils.isNotEmpty(structureJson) ? structureJson : "{}";
        JsonFormNode parse = null;
        try {
            parse = JSON.parse(structureJson, JsonFormNode.class);
        } catch (Exception e) {
            // ignore
            log.error("StructureJson 格式错误, JSON解析失败, 原文: {}", structureJson);
        }

        if (Objects.isNull(parse)) return new Object[0][];

        // 层级信息 & 解析
        List<Object[]> objs = new ArrayList<>();
        JsonFormNode.initLevel(parse);
        analyzeJsonNode(objs, parse);

        Object[][] array = new Object[objs.size()][];
        for (int i = 0; i < objs.size(); i++) {
            array[i] = objs.get(i);
        }

        return array;
    }

    private void analyzeJsonNode(List<Object[]> objs, JsonFormNode node) {
        if (StringUtils.isNotEmpty(node.getName())) {
            boolean require = Objects.nonNull(node.getParent()) && CollectionUtils.isNotEmpty(node.getParent().getRequired()) && node.getParent().getRequired().contains(node.getName());
            Object[] object = {node, require, node.getType(), node.getMock(), node.getTitle(), node.getDescription()};
            objs.add(object);
        }

        Map<String, JsonFormNode> properties = node.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            for (JsonFormNode property : properties.values()) {
                analyzeJsonNode(objs, property);
            }
        }

        JsonFormNode items = node.getItems();
        if (Objects.nonNull(items)) {
            analyzeJsonNode(objs, items);
        }
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        JsonFormNode node = new JsonFormNode();
        node.setName("field" + ApiDesignDialog.addFieldCount());
        node.setType("string");
        model.addRow(new Object[]{node, false, node.getType(), node.getMock(), node.getTitle(), node.getDescription()});
    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer centerRenderer) {

        table.getColumnModel().getColumn(0).setCellRenderer(new JsonCellRenderer());
        table.getColumnModel().getColumn(0).setCellEditor(new JsonCellEditor());
        table.getColumnModel().getColumn(0).setMinWidth(200);

        table.getColumnModel().getColumn(1).setCellRenderer(new CheckBoxRenderer());
        table.getColumnModel().getColumn(1).setCellEditor(new CheckBoxEditor());
        table.getColumnModel().getColumn(1).setPreferredWidth(40);

        ComboBox<String> fieldTypeComboBox = new ComboBox<>(Constants.Api.JSON_FIELD_TYPE);
        AutoCompleteDecorator.decorate(fieldTypeComboBox);
        fieldTypeComboBox.addActionListener(event -> changeFieldType(event, table));

        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellEditor(new ComboBoxCellEditor(fieldTypeComboBox));

        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(6).setCellRenderer(new JsonBtnCellRenderer(new JsonFormBtnPanel(table)));
        table.getColumnModel().getColumn(6).setCellEditor(new JsonBtnCellEditor(new JsonFormBtnPanel(table)));
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
    }

    @SuppressWarnings("all")
    private void changeFieldType(ActionEvent event, JTable table) {
        if (event.getSource() instanceof JComboBox) {
            JComboBox<String> combo = (JComboBox<String>) event.getSource();
            String selectedOption = (String) combo.getSelectedItem();
            if (StringUtils.isEmpty(selectedOption)) return;

            // 当前
            int editingRow = table.getEditingRow();
            if (editingRow < 0) return;
            // 数据节点
            JsonFormNode node = (JsonFormNode) table.getValueAt(editingRow, jsonColumn);
            node.setType(selectedOption);

            // array
            // 考虑表更新row会变动, 实时获取下级
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            JsonFormNode nextNode = nextNode(table, editingRow);
            if (ARRAY.equalsIgnoreCase(selectedOption)) {
                // 检测是否需要追加子级
                if (Objects.isNull(nextNode) || !(node.equals(nextNode.getParent()) && ITEMS.equalsIgnoreCase(nextNode.getName()))) {
                    // 需要追加子级 addItems
                    JsonFormNode itemsNode = new JsonFormNode();
                    itemsNode.setParent(node);
                    itemsNode.setLevel(node.getLevel() + 1);
                    itemsNode.setName(ITEMS);
                    itemsNode.setType("string");
                    itemsNode.setEditable(false);

                    // 添加到 items
                    addToParentItems(itemsNode);
                    model.insertRow(editingRow + 1, new Object[]{itemsNode, false, itemsNode.getType(), itemsNode.getMock(), itemsNode.getTitle(), itemsNode.getDescription()});
                    adjustColumnWidth(table, editingRow + 1);
                }
            } else if (Objects.nonNull(nextNode) && node.equals(nextNode.getParent()) && ITEMS.equalsIgnoreCase(nextNode.getName())) {
                // 检测是否需要移除多余array子级
                removeNodeAndAllChild(table, editingRow + 1);
            }

            // object
            // 考虑表更新row会变动, 实时获取下级
            int removeCount = 0;
            Map<Integer, JsonFormNode> childNodes = childNodes(table, node);
            for (Map.Entry<Integer, JsonFormNode> entry : childNodes.entrySet()) {
                Integer row = entry.getKey();
                JsonFormNode childNode = entry.getValue();

                boolean notObjectParent = !OBJECT.equalsIgnoreCase(selectedOption) && !ARRAY.equalsIgnoreCase(selectedOption) && Objects.nonNull(nextNode) && node.equals(nextNode.getParent());
                boolean notArrayParent = ARRAY.equalsIgnoreCase(selectedOption) && !ITEMS.equalsIgnoreCase(childNode.getName());

                if (notObjectParent || notArrayParent) {
                    removeNodeAndAllChild(table, row - removeCount);
                    removeCount++;
                }
            }
        }
    }

    private JsonFormNode nextNode(JTable table, int editingRow) {
        JsonFormNode nextNode = null;
        if (table.getRowCount() - 1 >= editingRow + 1) {
            nextNode = (JsonFormNode) table.getValueAt(editingRow + 1, jsonColumn);
        }

        return nextNode;
    }

    private Map<Integer, JsonFormNode> childNodes(JTable table, JsonFormNode node) {
        Map<Integer, JsonFormNode> children = new HashMap<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            JsonFormNode nextNode = (JsonFormNode) table.getValueAt(i, jsonColumn);
            if (node.equals(nextNode.getParent())) {
                children.put(i, nextNode);
            }
        }

        return children;
    }

    public String getDataJsonPreview(JTable table) {
        JsonFormNode rootNode = getRootNode(table);
        Map<String, Object> jsonPreview = convertJsonNodeToMap(rootNode);

        return com.alibaba.fastjson.JSON.toJSONString(jsonPreview, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
    }

    private Map<String, Object> convertJsonNodeToMap(JsonFormNode rootNode) {
        Map<String, Object> json = new HashMap<>();
        if ("array".equals(rootNode.getType())) {
            JsonFormNode items = rootNode.getItems();
            String type = items.getType();
            switch (type) {
                case "string": {
                    Map<String, Object> res = new HashMap<>();
                    res.put("1", "items1");
                    res.put("2", "items2");
                    return res;
                }
                case "number": {
                    Map<String, Object> res = new HashMap<>();
                    res.put("1", 10);
                    res.put("2", 20);
                    return res;
                }
                case "array":
                    return convertJsonNodeToMap(items.getItems());
                case "object":
                    rootNode = items;
                    break;
                case "boolean": {
                    Map<String, Object> res = new HashMap<>();
                    res.put("1", true);
                    res.put("2", false);
                    return res;
                }
                case "integer": {
                    Map<String, Object> res = new HashMap<>();
                    res.put("1", 1);
                    res.put("2", 2);
                    return res;
                }
                default:
                    break;
            }
        }

        Map<String, JsonFormNode> properties = rootNode.getProperties();
        for (Map.Entry<String, JsonFormNode> entry : properties.entrySet()) {
            String k = entry.getKey();
            JsonFormNode v = entry.getValue();
            String type = v.getType();
            // "string", "number", "array", "object", "boolean", "integer"
            switch (type) {
                case "string":
                    json.put(k, k);
                    break;
                case "number":
                    json.put(k, 1);
                    break;
                case "array":
                    Map<String, Object> array = convertJsonNodeToMap(v);
                    JsonFormNode items = v.getItems();
                    if (!(items.getType().equals("object") || items.getType().equals("array"))) {
                        // 基础类型
                        json.put(k, array.values());
                    } else {
                        // 嵌套类型
                        List<Map<String, Object>> arrayVal = new ArrayList<>();
                        arrayVal.add(array);
                        json.put(k, arrayVal);
                    }
                    break;
                case "object":
                    json.put(k, convertJsonNodeToMap(v));
                    break;
                case "boolean":
                    json.put(k, true);
                    break;
                case "integer":
                    json.put(k, 0);
                    break;
                default:
                    break;
            }
        }

        return json;
    }


    static class JsonCellRenderer extends DefaultTableCellRenderer {

        public JsonCellRenderer() {
            setHorizontalAlignment(JLabel.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JsonFormNode node = (JsonFormNode) value;

            // "参数", "必须", "类型", "mock", "中文名称", "备注", "操作"
            Object require = table.getCellEditor(row, 1).getCellEditorValue();
            node.setCurrentRequired(Objects.nonNull(require) ? (Boolean) require : false);

            Object mock = table.getCellEditor(row, 3).getCellEditorValue();
            node.setMock(Objects.nonNull(mock) ? mock.toString() : null);

            Object title = table.getCellEditor(row, 4).getCellEditorValue();
            node.setTitle(Objects.nonNull(title) ? title.toString() : StringUtils.EMPTY);

            Object description = table.getCellEditor(row, 5).getCellEditorValue();
            node.setDescription(Objects.nonNull(description) ? description.toString() : StringUtils.EMPTY);


            JLabel holder = new JLabel();
            JLabel nameLabel = new JLabel(node.getName());
            if (!node.isEditable()) {
                nameLabel.setForeground(RoundedLabel.BLUE);
            }

            if (Objects.nonNull(node.getParent()) && StringUtils.isNotEmpty(node.getParent().getName())) {
                String format = String.format("%" + node.getLevel() * 6 + "s", " ");
                holder.setText(format);
                nameLabel.setIcon(Icons.scaleToWidth(Icons.CHILD, 10));
            }

            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            panel.add(holder);
            panel.add(nameLabel);

            panel.setPreferredSize(rowHigh);
            return panel;
        }
    }

    static class JsonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField textField;
        private JsonFormNode jsonNode;

        public JsonCellEditor() {
            textField = new JTextField();
            textField.setHorizontalAlignment(JTextField.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JsonFormNode json = (JsonFormNode) value;
            jsonNode = json;

            textField.setText(json.getName());
            textField.setEditable(json.isEditable());
            textField.setPreferredSize(rowHigh);
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            jsonNode.setName(textField.getText());
            refreshPropertiesNode(jsonNode.getParent());

            return jsonNode;
        }

    }


    static class JsonBtnCellEditor extends DefaultCellEditor {
        private final JsonFormBtnPanel panel;

        public JsonBtnCellEditor(JsonFormBtnPanel panel) {
            super(new JTextField());
            this.setClickCountToStart(1);

            panel.setPreferredSize(rowHigh);
            this.panel = panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            setJsonFormBtnStatus(table, panel, row);
            table.clearSelection();
            if (row > table.getRowCount() - 1) {
                return null;
            }

            return panel;
        }
    }

    static class JsonBtnCellRenderer implements TableCellRenderer {
        private final JsonFormBtnPanel panel;

        public JsonBtnCellRenderer(JsonFormBtnPanel panel) {
            this.panel = panel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setJsonFormBtnStatus(table, panel, row);
            panel.setPreferredSize(rowHigh);
            return panel;
        }
    }

    private static void setJsonFormBtnStatus(JTable table, JsonFormBtnPanel panel, int row) {
        JsonFormNode node = (JsonFormNode) table.getValueAt(row, jsonColumn);
        String fieldType = node.getType();
        if (StringUtils.isEmpty(fieldType)) {
            Object valueAt = table.getValueAt(row, jsonFieldTypeColumn);
            fieldType = Objects.nonNull(valueAt) ? valueAt.toString() : StringUtils.EMPTY;
            if (StringUtils.isEmpty(fieldType)) return;
        }
        // 节点数据
        if (ARRAY.equalsIgnoreCase(fieldType)) {
            panel.getAddChild().setVisible(false);
            panel.getAdd().setVisible(true);
            panel.getRemove().setVisible(true);
        } else if (OBJECT.equalsIgnoreCase(fieldType)) {
            panel.getAddChild().setVisible(true);
            panel.getAdd().setVisible(true);
            panel.getRemove().setVisible(true);
        } else {
            panel.getAddChild().setVisible(false);
            panel.getAdd().setVisible(true);
            panel.getRemove().setVisible(true);
        }
        // items情况需要特殊控制
        if (ITEMS.equalsIgnoreCase(node.getName()) && Objects.nonNull(node.getParent()) && ARRAY.equalsIgnoreCase(node.getParent().getType())) {
            panel.getAddChild().setVisible(OBJECT.equalsIgnoreCase(fieldType));
            panel.getAdd().setVisible(false);
            panel.getRemove().setVisible(false);
        }
    }

    @Getter
    static class JsonFormBtnPanel extends JPanel {
        private final JButton add;
        private final JButton addChild;
        private final JButton remove;

        public JsonFormBtnPanel(JTable table) {
            setLayout(new FlowLayout());
            add = new JButton(Icons.scaleToWidth(Icons.API_ADD, 16));
            add.setActionCommand("add");
            add.setToolTipText("添加平级节点");
            add.setBorderPainted(false);
            add.setContentAreaFilled(false);
            add.setPreferredSize(new Dimension(22, 22));

            addChild = new JButton(Icons.scaleToWidth(Icons.API_ADD_CHILD, 16));
            addChild.setActionCommand("addChild");
            addChild.setToolTipText("添加下级节点");
            addChild.setBorderPainted(false);
            addChild.setContentAreaFilled(false);
            addChild.setPreferredSize(new Dimension(22, 22));

            remove = new JButton(Icons.scaleToWidth(Icons.API_DELETE, 16));
            remove.setActionCommand("remove");
            remove.setToolTipText("删除节点及其下级节点");
            remove.setBorderPainted(false);
            remove.setContentAreaFilled(false);
            remove.setPreferredSize(new Dimension(22, 22));

            add.setVisible(false);
            addChild.setVisible(false);
            remove.setVisible(false);

            add(add);
            add(addChild);
            add(remove);

            add.addActionListener(e -> addJsonNode(table));
            addChild.addActionListener(e -> addChildJsonNode(table));
            remove.addActionListener(e -> removeJsonNode(table));
        }

        public static void addJsonNode(JTable table) {
            int editRow = table.getEditingRow();
            Object valueAt = table.getValueAt(editRow, jsonColumn);
            if (!(valueAt instanceof JsonFormNode)) {
                return;
            }

            JsonFormNode json = (JsonFormNode) valueAt;
            int insertRow = editRow + 1;
            boolean lastRow = false;
            for (int row = editRow + 1; row < table.getRowCount(); row++) {
                JsonFormNode nextNode = (JsonFormNode) table.getValueAt(row, jsonColumn);
                if (json.getLevel() >= nextNode.getLevel()) {
                    insertRow = row;
                    break;
                }

                if (row == table.getRowCount() - 1) lastRow = true;
            }

            JsonFormNode node = (JsonFormNode) valueAt;
            JsonFormNode nextNode = new JsonFormNode();
            nextNode.setParent(node.getParent());
            nextNode.setLevel(node.getLevel());
            nextNode.setName("field" + ApiDesignDialog.addFieldCount());
            nextNode.setType("string");

            addToParentProperties(nextNode);

            insertRow = lastRow ? table.getRowCount() : insertRow;
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            boolean require = Objects.nonNull(node.getParent()) && CollectionUtils.isNotEmpty(node.getParent().getRequired()) && node.getParent().getRequired().contains(nextNode.getName());
            model.insertRow(insertRow, new Object[]{nextNode, require, nextNode.getType(), nextNode.getMock(), nextNode.getTitle(), nextNode.getDescription()});

            table.scrollRectToVisible(table.getCellRect(insertRow, 0, true));
        }

        public static void addChildJsonNode(JTable table) {
            int editRow = table.getEditingRow();
            Object valueAt = table.getValueAt(editRow, jsonColumn);
            if (!(valueAt instanceof JsonFormNode)) {
                return;
            }

            JsonFormNode node = (JsonFormNode) valueAt;
            JsonFormNode childNode = new JsonFormNode();
            childNode.setParent(node);
            childNode.setLevel(node.getLevel() + 1);
            childNode.setType("string");
            childNode.setName("field" + ApiDesignDialog.addFieldCount());

            addToParentProperties(childNode);

            int insertRow = editRow + 1;
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            boolean require = CollectionUtils.isNotEmpty(node.getRequired()) && node.getRequired().contains(childNode.getName());
            model.insertRow(insertRow, new Object[]{childNode, require, childNode.getType(), childNode.getMock(), childNode.getTitle(), childNode.getDescription()});

            adjustColumnWidth(table, insertRow);
            table.scrollRectToVisible(table.getCellRect(insertRow, 0, true));
        }

        public static void removeJsonNode(JTable table) {
            int editingRow = table.getEditingRow();
            // 必须主动退出编辑状态, 因为btnPanel是个特殊单元格, JTable原生并不支持
            // 不主动退出编辑状态的话, 删除最后一行时会数组越界, 原因是 JsonBtnCellEditor 会返回编辑状态下的界面按钮, 但是行已被删除因此返回失败导致数组越界, 因此主动退出编辑状态即可
            table.editingStopped(null);

            int logout = Messages.showDialog("删除当前节点及其下级节点 ?", "节点删除", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.DELETE, 60));
            if (logout == Messages.YES) {
                removeNodeAndAllChild(table, editingRow);
                table.repaint();
            }
        }

    }

    public static void adjustColumnWidth(JTable table, int row) {
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn tableColumn = columnModel.getColumn(JsonTableWrap.jsonColumn);
        int preferredWidth = tableColumn.getMinWidth();
        int maxWidth = tableColumn.getMaxWidth();

        TableCellRenderer cellRenderer = table.getCellRenderer(row, JsonTableWrap.jsonColumn);
        Component c = table.prepareRenderer(cellRenderer, row, JsonTableWrap.jsonColumn);
        int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
        preferredWidth = Math.max(preferredWidth, width);

        // 如果宽度超过最大宽度，则设置为最大宽度
        if (preferredWidth >= maxWidth) {
            preferredWidth = maxWidth;
        }

        // 重新设置编辑器
        tableColumn.setPreferredWidth(preferredWidth);
        table.repaint();
    }

    private static void removeNodeAndAllChild(JTable table, int removeRow) {
        removeRow = Math.max(0, removeRow);
        removeRow = Math.min(table.getRowCount(), removeRow);

        if (table.getRowCount() == 0) return;
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        Object valueAt = table.getValueAt(removeRow, jsonColumn);
        if (!(valueAt instanceof JsonFormNode)) {
            return;
        }

        List<Integer> childRows = new ArrayList<>();
        JsonFormNode removeNode = (JsonFormNode) valueAt;
        List<JsonFormNode> removeNodes = Lists.newArrayList();
        for (int row = removeRow + 1; row < table.getRowCount(); row++) {
            JsonFormNode nextNode = (JsonFormNode) table.getValueAt(row, jsonColumn);
            if (removeNode.equals(nextNode.getParent()) || removeNodes.contains(nextNode.getParent())) {
                childRows.add(row);
                removeNodes.add(nextNode);
            }
        }

        for (int i = 0; i < childRows.size(); i++) {
            // remove后row会上移, 使用 childRows.get(0) 做连续删除即可
            model.removeRow(childRows.get(0));
        }

        model.removeRow(removeRow);

        // update parentNode properties
        removeNodes.add(removeNode);
        for (JsonFormNode node : removeNodes) {
            removeFromParentPropertiesAndItems(node);
        }
    }

    private static void removeFromParentPropertiesAndItems(JsonFormNode node) {
        JsonFormNode parent = node.getParent();
        if (Objects.nonNull(parent) && MapUtils.isNotEmpty(parent.getProperties())) {
            Map<String, JsonFormNode> properties = parent.getProperties();
            properties.remove(node.getName());
        }
        if (Objects.nonNull(parent) && Objects.nonNull(parent.getItems()) && MapUtils.isNotEmpty(parent.getItems().getProperties())) {
            Map<String, JsonFormNode> properties = parent.getItems().getProperties();
            properties.remove(node.getName());
            if (MapUtils.isEmpty(properties)) {
                parent.setItems(null);
            }
        }
    }

    private static void addToParentProperties(JsonFormNode node) {
        JsonFormNode parent = node.getParent();
        if (Objects.nonNull(parent)) {
            Map<String, JsonFormNode> properties = ObjectUtils.defaultIfNull(parent.getProperties(), new HashMap<>());
            properties.put(node.getName(), node);
            parent.setProperties(properties);
        }
    }

    private static void addToParentItems(JsonFormNode node) {
        JsonFormNode parent = node.getParent();
        if (Objects.nonNull(parent)) {
            parent.setItems(node);
            parent.setProperties(new HashMap<>());
        }
    }

    private static void refreshPropertiesNode(JsonFormNode node) {
        if (Objects.isNull(node)) {
            return;
        }

        List<String> required = new ArrayList<>();
        Map<String, JsonFormNode> properties = node.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            Map<String, JsonFormNode> renameProperties = new HashMap<>();
            for (JsonFormNode property : properties.values()) {
                renameProperties.put(property.getName(), property);
                if (property.isCurrentRequired()) required.add(property.getName());
            }

            node.setProperties(renameProperties);
            node.setRequired(required);
        }
    }
}
