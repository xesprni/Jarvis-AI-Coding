package com.qihoo.finance.lowcode.gentracker.ui.table;

import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import com.qihoo.finance.lowcode.common.ui.table.BaseJTableWrap;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.entity.Callback;
import com.qihoo.finance.lowcode.gentracker.entity.SaveFile;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.FileDiffDialog;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * FileTableWrap
 *
 * @author fengjinfu-jk
 * date 2024/1/16
 * @version 1.0.0
 * @apiNote FileTableWrap
 */
public class FileTableWrap implements BaseJTableWrap {
    private final List<SaveFile> saveFiles;
    private final FileDiffDialog dialog;
    private static final Dimension rowHigh = new Dimension(-1, 26);

    public FileTableWrap(FileDiffDialog dialog, List<SaveFile> saveFiles) {
        this.dialog = dialog;
        this.saveFiles = saveFiles;
    }

    @Override
    public String[] getTableHeaders() {
        return new String[]{"", "", "文件名称", "包路径", /*"处理方式",*/ "选项"};
    }

    @Override
    public Object[][] getDefaultTableData() {
        List<Object[]> objects = new ArrayList<>();
        for (SaveFile saveFile : saveFiles) {
            Callback callback = saveFile.getCallback();
            objects.add(new Object[]{
                    saveFile, saveFile.isModify() ? "修改" : "新增",
                    callback.getFileName(),
                    getPackage(callback.getSavePath() + "/" + callback.getFileName())
            });
        }

        return objects.toArray(Object[][]::new);
    }

    private Object getPackage(String filePath) {
        if (filePath.contains("src/main/java/")) {
            String packagePath = StringUtils.substringAfter(filePath, "src/main/java/").replaceAll("/", ".");
            if (packagePath.startsWith(".")) {
                packagePath = packagePath.substring(1);
            }
            return packagePath;
        }

        return filePath;
    }

    @Override
    public void addRow(ActionEvent e, JTable table) {

    }

    public static boolean existModify(JTable fileTable) {
        for (int i = 0; i < fileTable.getModel().getRowCount(); i++) {
            SaveFile saveFile = (SaveFile) fileTable.getModel().getValueAt(i, FileTableWrap.FILE_IDX);
            if (saveFile.getIsModify()) return true;
        }

        return false;
    }

    @Override
    public void configColumnProperties(JTable table, DefaultTableCellRenderer leftCellRenderer) {
        table.getColumnModel().getColumn(FILE_IDX).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(FILE_IDX).setCellEditor(new TextCellEditor());

        table.getColumnModel().getColumn(SAVE_TYPE_IDX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (component instanceof JLabel) {
                    if (value.equals("新增")) {
                        component.setForeground(JBColor.GREEN);
                    } else {
                        component.setForeground(JBColor.BLUE);
                    }
                }
                return component;
            }
        });

        table.getColumnModel().getColumn(SAVE_TYPE_IDX).setCellEditor(new TextCellEditor(false, SwingConstants.CENTER));
        table.getColumnModel().getColumn(SAVE_TYPE_IDX).setPreferredWidth(50);

        leftCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(FILE_NAME_IDX).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(FILE_NAME_IDX).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(FILE_NAME_IDX).setPreferredWidth(250);

        table.getColumnModel().getColumn(FILE_PATH_IDX).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(FILE_PATH_IDX).setCellEditor(new TextCellEditor(false, SwingConstants.LEFT));
        table.getColumnModel().getColumn(FILE_PATH_IDX).setPreferredWidth(300);

/*        table.getColumnModel().getColumn(HANDLER_IDX).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(HANDLER_IDX).setCellEditor(new TextCellEditor());
        table.getColumnModel().getColumn(HANDLER_IDX).setPreferredWidth(80);*/

        table.getColumnModel().getColumn(BTN_IDX).setCellRenderer(new BtnCellRenderer(new BtnPanel(dialog, table)));
        table.getColumnModel().getColumn(BTN_IDX).setCellEditor(new BtnCellEditor(new BtnPanel(dialog, table)));
        table.getColumnModel().getColumn(BTN_IDX).setPreferredWidth(180);

        table.removeColumn(table.getColumnModel().getColumn(FILE_IDX));
    }

    public static final int SAVE_TYPE_IDX = 1;
    public static final int FILE_NAME_IDX = 2;
    public static final int FILE_PATH_IDX = 3;
    public static final int HANDLER_IDX = 4;
    public static final int BTN_IDX = 4;
    public static final int FILE_IDX = 0;


    static class BtnCellEditor extends DefaultCellEditor {
        private final BtnPanel panel;

        public BtnCellEditor(BtnPanel panel) {
            super(new JTextField());
            this.setClickCountToStart(1);

            panel.setPreferredSize(rowHigh);
            this.panel = panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            repaintBtn(table, panel, row);
            table.clearSelection();
            if (row > table.getRowCount() - 1) {
                return null;
            }

            return panel;
        }
    }

    static class BtnCellRenderer implements TableCellRenderer {
        private final BtnPanel panel;

        public BtnCellRenderer(BtnPanel panel) {
            panel.setPreferredSize(rowHigh);
            this.panel = panel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            repaintBtn(table, panel, row);
            return panel;
        }
    }

    private static void repaintBtn(JTable table, BtnPanel panel, int row) {
        SaveFile saveFile = (SaveFile) table.getModel().getValueAt(row, FILE_IDX);
//        panel.getOverwrite().setVisible(saveFile.isModify());
        panel.getOverwrite().setVisible(true);
        panel.getIgnore().setVisible(true);
    }

    @Getter
    static class BtnPanel extends JPanel {
        private final FileDiffDialog dialog;
        private final ActionLink showDiff;
        private final ActionLink overwrite;
        private final ActionLink ignore;
        private final SaveFile.SaveOptions saveOptions = new SaveFile.SaveOptions();

        public BtnPanel(FileDiffDialog dialog, JTable table) {
            this.dialog = dialog;

            setLayout(new FlowLayout());
            showDiff = new ActionLink("查看变更", e -> {
                showDiff(table);
            });
            overwrite = new ActionLink("覆盖", e -> {
                overwrite(table);
            });
            ignore = new ActionLink("忽略", e -> {
                ignore(table);
            });

            overwrite.setVisible(false);
            ignore.setVisible(false);

            add(showDiff);
            add(overwrite);
            add(ignore);
        }

        private void ignore(JTable table) {
            SaveFile rowFile = getRowFile(table);
            if (checkMistake("忽略文件", String.format("%s\n确认忽略该文件？", rowFile.getCallback().getFileName()))) {
                return;
            }

//            rowFile.write(false, saveOptions);
            SaveFile saveFile = (SaveFile) table.getModel().getValueAt(table.getEditingRow(), FileTableWrap.FILE_IDX);
            saveFile.setIgnore(true);

            // 移除已处理行
            removeRow(table, table.getEditingRow());
            dialog.updateDialogStatus();
        }

        private void overwrite(JTable table) {
            SaveFile rowFile = getRowFile(table);
            if (checkMistake("覆盖文件", String.format("%s\n确认覆盖该文件？", rowFile.getCallback().getFileName()))) {
                return;
            }

            rowFile.write(true, saveOptions);
            // 移除已处理行
            removeRow(table, table.getEditingRow());
            dialog.updateDialogStatus();
        }

        private boolean checkMistake(String title, String warnMsg) {
            int check = Messages.showDialog("\n" + warnMsg, title, new String[]{"是", "否"}, 0, Icons.scaleToWidth(Icons.WARN, 60));
            return check != 0;
        }

        private void showDiff(JTable table) {
            SaveFile rowFile = getRowFile(table);
            rowFile.showDiffWindows();
        }

        private SaveFile getRowFile(JTable table) {
            int editRow = table.getEditingRow();
            return (SaveFile) table.getModel().getValueAt(editRow, FILE_IDX);
        }

        private void removeRow(JTable table, int row) {
            table.editingStopped(null);
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.removeRow(row);
        }
    }
}
