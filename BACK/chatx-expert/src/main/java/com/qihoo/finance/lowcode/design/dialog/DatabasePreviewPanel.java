package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbField;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbFieldTypeConfig;
import com.qihoo.finance.lowcode.design.dto.rdb.RdbIndex;
import com.qihoo.finance.lowcode.design.ui.DatabaseBasePanel;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.qihoo.finance.lowcode.common.constants.Constants.DB_COLUMN.DEFAULT_VALUES;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
@Getter
public class DatabasePreviewPanel extends DatabaseBasePanel {

    /**
     * 预览的收尾配置
     */
    private Map<Integer, String> sqlPreviewMap;

    private Editor sqlPreviewEditor;
    /**
     * 列配置数据
     */
    private List<RdbField> rdbFields;
    private List<RdbField> droppedRdbFields;
    private List<RdbIndex> rdbIndexes;
    private List<RdbIndex> droppedRdbIndexes;

    public DatabasePreviewPanel(@NotNull Project project) {
        super(project);
    }

    public void init(List<RdbField> rdbFields, List<RdbField> droppedRdbFields, List<RdbIndex> rdbIndexes
            , List<RdbIndex> droppedRdbIndexes, Map<Integer, String> sqlPreviewMap) {
        this.rdbFields = rdbFields;
        this.droppedRdbFields = droppedRdbFields;
        this.rdbIndexes = rdbIndexes;
        this.droppedRdbIndexes = droppedRdbIndexes;
        this.sqlPreviewMap = sqlPreviewMap;
    }

    @Override
    public Component createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        sqlPreviewEditor = EditorComponentUtils.createEditorPanel(project, LightVirtualType.SQL);
        updateSqlPreviewText();
        panel.add(sqlPreviewEditor.getComponent(), BorderLayout.CENTER);
        return panel;
    }


    public void updateSqlPreviewText() {
        // 区分新增编辑
        if (isEdit()) {
            updateModifySqlPreviewText();
        } else {
            updateCreateSqlPreviewText();
        }
    }

    protected void updateModifySqlPreviewText() {
        /*
        ALTER TABLE `permissions_group_user_relation`
        ADD COLUMN `field_name` bigint(123) UNSIGNED NOT NULL AUTO_INCREMENT DEFAULT '' COMMENT '注释,
        ADD COLUMN `field_name2` varchar(123) UNSIGNED NOT NULL AUTO_INCREMENT DEFAULT '' COMMENT '注释2;
         */
        StringBuilder sqlPreviewText = new StringBuilder();
        if (!rdbFields.isEmpty()) {
            buildModifyColumns(sqlPreviewText);
            buildModifyPrimaryKeys(sqlPreviewText);
            buildModifyIndexKeys(sqlPreviewText);

            if (sqlPreviewText.length() > 0) {
                sqlPreviewText.insert(0, String.format("ALTER TABLE `%s` \n", DataContext.getInstance(project).getSelectDbTable().getTableName()));
                sqlPreviewText.deleteCharAt(sqlPreviewText.lastIndexOf(",")).deleteCharAt(sqlPreviewText.lastIndexOf("\n")).append(";\n\n");
            }
        }

        String prefix = sqlPreviewMap.get(0);
        if (StringUtils.isNotEmpty(prefix)) {
            sqlPreviewText.append(prefix).append("\n");
        }
        String suffix = sqlPreviewMap.get(999);
        if (StringUtils.isNotEmpty(suffix)) {
            sqlPreviewText.append(suffix).append("\n");
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            sqlPreviewEditor.getDocument().setReadOnly(false);
            sqlPreviewEditor.getDocument().setText(sqlPreviewText.toString());
            sqlPreviewEditor.getDocument().setReadOnly(true);
        });
    }

    private void buildModifyPrimaryKeys(StringBuilder sqlPreviewText) {
        /*
        DROP PRIMARY KEY,
        ADD PRIMARY KEY (`id`, `id2`) USING BTREE;
         */
        boolean changed = false;
        for (RdbField field : rdbFields) {
            if (field.isExistsInDb()) {
                RdbField fieldBackup = JSON.parse(field.getDbColumnBackup(), RdbField.class);
                if (field.isPk() != fieldBackup.isPk()) {
                    changed = true;
                    break;
                }
            }
        }

        boolean newPrimary = rdbFields.stream().anyMatch(field -> !field.isExistsInDb() && field.isPk());
        if (newPrimary || changed) {
            sqlPreviewText.append("DROP PRIMARY KEY,").append("\n");
            String primaryKeyStr = rdbFields.stream().filter(RdbField::isPk).map(RdbField::getFieldName).map(field -> "`" + field + "`").collect(Collectors.joining(", "));
            sqlPreviewText.append(String.format("ADD PRIMARY KEY (%s) USING BTREE,", primaryKeyStr)).append("\n");
        }
    }

    protected void updateCreateSqlPreviewText() {
        List<String> primaryKeys = new LinkedList<>();
        StringBuilder sqlPreviewText = new StringBuilder();
        if (!rdbFields.isEmpty()) {
            sqlPreviewText.append(sqlPreviewMap.get(0)).append(" (").append("\n");
            buildColumns(sqlPreviewText, primaryKeys);
            buildPrimaryKeys(sqlPreviewText, primaryKeys);
            buildIndexKeys(sqlPreviewText, false);
            sqlPreviewText.deleteCharAt(sqlPreviewText.length() - 2);
            sqlPreviewText.append(")").append(sqlPreviewMap.get(999)).append("\n");
        } else {
            sqlPreviewText.append(sqlPreviewMap.get(0)).append("\n");
            sqlPreviewText.append(sqlPreviewMap.get(999)).append("\n");
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            sqlPreviewEditor.getDocument().setReadOnly(false);
            sqlPreviewEditor.getDocument().setText(sqlPreviewText.toString());
            sqlPreviewEditor.getDocument().setReadOnly(true);
        });
    }

    private void buildModifyColumns(StringBuilder sqlPreviewText) {
        // add 字段，需要放到modify之前，否则会导致「modify after 不存在的字段」的情况
        List<RdbField> addFields = rdbFields.stream().filter(f -> !f.isExistsInDb())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(addFields)) {
            for (RdbField field : addFields) {
                // ADD COLUMN `field_name` bigint(123) UNSIGNED NOT NULL AUTO_INCREMENT DEFAULT '' COMMENT '注释,
                sqlPreviewText.append(String.format("ADD COLUMN `%s` ", field.getFieldName()));
                // 字段名
                sqlPreviewText.append(fieldType(field)).append(" ");
                // 其余字段属性
                sqlPreviewText.append(buildColumnsProperties(field));
                // 换行
                sqlPreviewText.append(", \n");
            }
        }
        // modify 字段
        List<RdbField> modifyFields = rdbFields.stream().filter(RdbField::isExistsInDb).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(modifyFields)) {
            // 比对检查是否更改
            for (RdbField rdbField : modifyFields) {
                RdbField fieldBackup = JSON.parse(rdbField.getDbColumnBackup(), RdbField.class);
                boolean modify = false;
                // 字段类型
                if (compareObjModify(fieldBackup.getFieldType(), rdbField.getFieldType())) {
                    modify = true;
                }
                // 长度
                if (compareObjModify(fieldBackup.getFieldLength(), rdbField.getFieldLength())) {
                    modify = true;
                }
                // 小数点
                if (compareObjModify(fieldBackup.getFieldPrecision(), rdbField.getFieldPrecision())) {
                    modify = true;
                }
                // 非空
                if (!fieldBackup.isNotNull() == rdbField.isNotNull()) {
                    modify = true;
                }
                // 主键 有primaryColumn确定是否变化
                // 自增
                if (!fieldBackup.isAutoIncr() == rdbField.isAutoIncr()) {
                    modify = true;
                }
                // 无符号
                if (!fieldBackup.isUnsigned() == rdbField.isUnsigned()) {
                    modify = true;
                }
                // 默认值
                if (compareObjModify(fieldBackup.getFieldDefault(), rdbField.getFieldDefault())) {
                    modify = true;
                }
                // 注释
                if (compareObjModify(fieldBackup.getFieldComment(), rdbField.getFieldComment())) {
                    modify = true;
                }
                if (StringUtils.isNotBlank(rdbField.getFieldOrder())) {
                    modify = true;
                }
                // 名称
                boolean changeName = !fieldBackup.getFieldName().equals(rdbField.getFieldName());
                if (changeName) {
                    sqlPreviewText.append(String.format("CHANGE COLUMN `%s` `%s` ", fieldBackup.getFieldName(), rdbField.getFieldName()));
                    sqlPreviewText.append(fieldType(rdbField)).append(" ");
                    sqlPreviewText.append(buildColumnsProperties(rdbField)).append(",\n");
                } else if (modify) {
                    sqlPreviewText.append(String.format("MODIFY COLUMN `%s` ", rdbField.getFieldName()));
                    sqlPreviewText.append(fieldType(rdbField)).append(" ");
                    sqlPreviewText.append(buildColumnsProperties(rdbField)).append(",\n");
                }
            }
        }
        // drop 字段
        if (!droppedRdbFields.isEmpty()) {
            for (RdbField field : droppedRdbFields) {
                sqlPreviewText.append(String.format("DROP COLUMN `%s`", field.getFieldName())).append(",\n");
            }
        }
    }

    private boolean compareObjModify(Object org, Object target) {
        if (Objects.nonNull(org)) {
            return !org.equals(target);
        } else return Objects.nonNull(target);
    }

    public static String escapeString(String str) {
        return str.replace("'", "\\'");
    }

    private StringBuilder buildColumnsProperties(RdbField fieldEntity) {
        StringBuilder sqlPreviewText = new StringBuilder();
        if (fieldEntity.isUnsigned()) {
            sqlPreviewText.append("UNSIGNED").append(" ");
        }

        // 非空
        if (fieldEntity.isNotNull()) {
            sqlPreviewText.append("NOT NULL").append(" ");
        } else {
            sqlPreviewText.append("NULL").append(" ");
        }

        // 自增
        if (fieldEntity.isAutoIncr()) {
            sqlPreviewText.append("AUTO_INCREMENT").append(" ");
        }

        // 默认值
        if (fieldEntity.getFieldDefault() != null) {
            String fieldDefault = String.valueOf(fieldEntity.getFieldDefault());
            if (StringUtils.isNotBlank(fieldDefault)) {
                if ("EMPTY STRING".equals(fieldDefault)) {
                    sqlPreviewText.append("DEFAULT ").append("''").append(" ");
                } else if (DEFAULT_VALUES.contains(fieldDefault)) {
                    sqlPreviewText.append("DEFAULT ").append(fieldEntity.getFieldDefault()).append(" ");
                } else if ((fieldDefault.startsWith("'") && fieldDefault.endsWith("'"))) {
                    String fieldDefaultVal = fieldEntity.getFieldDefault().toString().substring(1, fieldDefault.length() - 1);
                    sqlPreviewText.append("DEFAULT '").append(escapeString(fieldDefaultVal)).append("' ");
                } else {
                    sqlPreviewText.append("DEFAULT '").append(escapeString(fieldEntity.getFieldDefault().toString())).append("' ");
                }
            }
        }

        // 注释
        if (StringUtils.isNotBlank(fieldEntity.getFieldComment())) {
            sqlPreviewText.append("COMMENT '").append(escapeString(fieldEntity.getFieldComment())).append("' ");
        }
        // 顺序
        if (isEdit()) {
            if (StringUtils.isNotBlank(fieldEntity.getFieldOrder())) {
                sqlPreviewText.append(" ").append(fieldEntity.getFieldOrder());
            }
        }
        return sqlPreviewText;
    }

    protected void buildColumns(StringBuilder sqlPreviewText, List<String> primaryKeys) {
        rdbFields.forEach(rdbField -> {
            sqlPreviewText.append("   ");
            if (StringUtils.isNotBlank(rdbField.getFieldName())) {
                sqlPreviewText.append("`").append(rdbField.getFieldName()).append("`").append(" ");
            }
            // 拼接时判断是否需要长度和精度
            sqlPreviewText.append(fieldType(rdbField)).append(" ");
            // 其余属性
            sqlPreviewText.append(buildColumnsProperties(rdbField));

            // 主键信息
            if (rdbField.isPk() && StringUtils.isNotBlank(rdbField.getFieldName())) {
                primaryKeys.add(rdbField.getFieldName());
            }
            sqlPreviewText.append(", \n");
        });

    }

    private String fieldType(RdbField rdbField) {
        // 拼接时判断是否需要长度和精度
        String fieldType = StringUtils.defaultString(rdbField.getFieldType());
        fieldType = fieldType.replace(" unsigned", "");
        String fieldTypeLength = "";
        RdbFieldTypeConfig fieldTypeConfig = FieldTypeMatch.FIELD_TYPE_CONFIG.get(fieldType);
        if (fieldTypeConfig != null) {
            // 拼接字段长度
            if (fieldTypeConfig.isNeedFieldLength() && (rdbField.getFieldLength() != null)) {
                // 拼接字段精度
                if (fieldTypeConfig.isNeedFieldPrecision() && (rdbField.getFieldPrecision() != null)) {
                    fieldTypeLength = String.format("%s(%d, %d)", fieldType, rdbField.getFieldLength(), rdbField.getFieldPrecision());
                } else {
                    fieldTypeLength = String.format("%s(%d)", fieldType, rdbField.getFieldLength());
                }
            } else {
                fieldTypeLength = fieldType;
            }
        }
        return fieldTypeLength;
    }

    protected void buildPrimaryKeys(StringBuilder sqlPreviewText, List<String> primaryKeys) {
        if (CollectionUtils.isNotEmpty(primaryKeys)) {
            sqlPreviewText.append("   PRIMARY KEY (");
            primaryKeys.forEach(primaryKey -> {
                sqlPreviewText.append("`").append(primaryKey).append("`").append(",");
            });
            sqlPreviewText.deleteCharAt(sqlPreviewText.lastIndexOf(","));
            sqlPreviewText.append(")").append(",\n");
//            sqlPreviewText.append(") USING BTREE").append(",\n");
        }
    }

    private void buildModifyIndexKeys(StringBuilder sqlPreviewText) {
        /*
         DROP INDEX `uk_product_code`,
         ADD INDEX `uk_product_code`(product_code,code) USING BTREE ,
         ADD INDEX `a_index`(id,no) USING BTREE COMMENT 'abc';
         */

        // DROP
        for (RdbIndex rdbIndex : droppedRdbIndexes) {
            sqlPreviewText.append(String.format("DROP INDEX `%s`,", rdbIndex.getIndexName())).append("\n");
        }

        // MODIFY(cast DROP)
        List<RdbIndex> dbRdbIndexes = rdbIndexes.stream().filter(RdbIndex::isDbIndex).collect(Collectors.toList());
        for (RdbIndex index : dbRdbIndexes) {
            RdbIndex indexBackup = JSON.parse(index.getDbIndexBackup(), RdbIndex.class);
            boolean change = false;
            if (!index.getIndexName().equals(indexBackup.getIndexName())) {
                change = true;
            }
            if (!index.getIndexType().equals(indexBackup.getIndexType())) {
                change = true;
            }
            if (!index.getIndexMethod().equals(indexBackup.getIndexMethod())) {
                change = true;
            }
            if (!index.getIndexField().equals(indexBackup.getIndexField())) {
                change = true;
            }
            if (!index.getIndexComment().equals(indexBackup.getIndexComment())) {
                change = true;
            }

            if (change) {
                index.setDbIndex(false);
                sqlPreviewText.append(String.format("DROP INDEX `%s`,", indexBackup.getIndexName())).append("\n");

                // 由于已经设定为非DbIndex, 需要将原始索引删除
                droppedRdbIndexes.add(indexBackup);
            }
        }

        // ADD
        buildIndexKeys(sqlPreviewText, true);
    }

    protected void buildIndexKeys(StringBuilder sqlPreviewText, boolean isEdit) {
        List<RdbIndex> needProcessIndexes = rdbIndexes;
        if (isEdit()) {
            needProcessIndexes = needProcessIndexes.stream().filter(i -> !i.isDbIndex()).collect(Collectors.toList());
        }

        for (RdbIndex index : needProcessIndexes) {
            if (isEdit) {
                sqlPreviewText.append("ADD ");
            } else {
                sqlPreviewText.append("  ");
            }
            if (StringUtils.isNotBlank(index.getIndexType())) {
                sqlPreviewText.append("NORMAL".equals(index.getIndexType()) ? "" : " " + index.getIndexType()).append(" INDEX").append(" ");
            }

            if (StringUtils.isNotBlank(index.getIndexName())) {
                sqlPreviewText.append("`").append(index.getIndexName()).append("`").append("(");
            }

            if (StringUtils.isNotBlank(index.getIndexField())) {
                sqlPreviewText.append(index.getIndexField().replaceAll("'", "`")).append(")").append(" ");
            }

            if (StringUtils.isNotBlank(index.getIndexMethod())) {
                sqlPreviewText.append("USING ").append(index.getIndexMethod()).append(" ");
            }

            if (StringUtils.isNotBlank(index.getIndexComment())) {
                sqlPreviewText.append("COMMENT '").append(escapeString(index.getIndexComment())).append("'");
            }
            sqlPreviewText.append(",\n");
        }
    }
}
