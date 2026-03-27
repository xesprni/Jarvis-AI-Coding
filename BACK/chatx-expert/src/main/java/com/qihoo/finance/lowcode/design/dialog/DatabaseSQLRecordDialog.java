package com.qihoo.finance.lowcode.design.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.entity.base.PageDTO;
import com.qihoo.finance.lowcode.common.entity.dto.TableChangeRecordDTO;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.PageComponentUtils;
import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * DatabaseDDLDialog
 *
 * @author fengjinfu-jk
 * date 2023/10/11
 * @version 1.0.0
 * @apiNote DatabaseDDLDialog
 */
public class DatabaseSQLRecordDialog extends DialogWrapper {
    public DatabaseSQLRecordDialog(@NotNull Project project) {
        super(project);
        initComponents(project);
    }

    private void initComponents(Project project) {
        DataContext dataContext = DataContext.getInstance(project);
        DatabaseNode database = dataContext.getSelectDatabase();
        MySQLTableNode table = dataContext.getSelectDbTable();

        int page = 1;
        int pageSize = 3;

        dialogPanel = new JPanel(new BorderLayout());

        JLabel tips = new JLabel(String.format(" %s.%s   表变更记录", database.getName(), table.getTableName()));
        tips.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        tips.setIcon(Icons.scaleToWidth(Icons.TABLE2, 20));

        JPanel tipPanel = new JPanel();
        tipPanel.add(tips);
        tipPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        dialogPanel.add(tipPanel, BorderLayout.NORTH);

        contentPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        contentPanel.setPreferredSize(new Dimension(1000, 600));

        PageDTO<TableChangeRecordDTO> records = DatabaseDesignUtils.queryDdlRecord(database, table, page, pageSize);
        addRecordPanel(project, records);
        dialogPanel.add(contentPanel, BorderLayout.CENTER);

        pagePanel = new JPanel();
        pagePanel.add(createPagePanel(project, records, page, pageSize));
        dialogPanel.add(pagePanel, BorderLayout.SOUTH);

        init();
        setTitle(GlobalDict.TITLE_INFO + "-SQL执行记录");
        this.setModal(false);
    }

    private JPanel createPagePanel(Project project, PageDTO<TableChangeRecordDTO> records, int page, int pageSize) {
        DataContext dataContext = DataContext.getInstance(project);
        return PageComponentUtils.pagePanel(records, page, pageSize, (newPage, newPageSize) -> {
            PageDTO<TableChangeRecordDTO> newRecords =
                    DatabaseDesignUtils.queryDdlRecord(dataContext.getSelectDatabase(), dataContext.getSelectDbTable(), newPage, newPageSize);

            addRecordPanel(project, newRecords);
            pagePanel.removeAll();
            pagePanel.add(createPagePanel(project, newRecords, newPage, newPageSize));
        });
    }

    private void addRecordPanel(Project project, PageDTO<TableChangeRecordDTO> records) {
        contentPanel.removeAll();

        List<TableChangeRecordDTO> rows = records.getRows();
        if (CollectionUtils.isEmpty(rows)) {
            JLabel noneRecord = new JLabel(Icons.scaleToWidth(Icons.HOLDER, 250));
            noneRecord.setBorder(BorderFactory.createEmptyBorder(200, 0, 0, 0));

            contentPanel.add(noneRecord);
            return;
        }

        // 3 * 150
        for (TableChangeRecordDTO record : rows) {
            JPanel recordComponents = createRecordComponents(project, record, new Dimension(750, 450 / rows.size()));
            recordComponents.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            contentPanel.add(recordComponents);
        }
    }

    private JPanel createRecordComponents(@NotNull Project project, TableChangeRecordDTO record, Dimension dimension) {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        JPanel contentPanel = new JPanel();

        String time = LocalDateUtils.convertToPatternString(record.getDateCreated(), LocalDateUtils.FORMAT_DATE_TIME);
        JLabel descLabel = new JLabel(String.format("%s    %s    version:  %s", time, record.getCreatedBy(), record.getChangeVersion()));
        descLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        descLabel.setIcon(AllIcons.Providers.Mysql);

        JPanel ddlContent = new JPanel(new BorderLayout());
        ddlContent.setPreferredSize(dimension);
        Editor editor = EditorComponentUtils.createEditorPanel(project, LightVirtualType.SQL);
        ddlContent.add(editor.getComponent(), BorderLayout.CENTER);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // 添加监控事件
            String sql = StringUtils.defaultString(record.getExecuteSql()).trim();
            if (sql.endsWith("\n")) sql = sql.substring(0, sql.length() -1);
            editor.getDocument().setText(sql);
            editor.getDocument().setReadOnly(true);
        });

        //======== this ========
        contentPanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

        //---- descLabel ----
        contentPanel.add(descLabel, new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));

        //======== ddlContent ========
        contentPanel.add(ddlContent, new GridConstraints(2, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on

        return contentPanel;
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPanel;
    private JPanel contentPanel;
    private JPanel pagePanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        // 返回一个空的Action数组
        return new Action[]{};
    }

    @Override
    protected @NotNull Action getHelpAction() {
        // 返回一个空的Action对象
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
    }
}
