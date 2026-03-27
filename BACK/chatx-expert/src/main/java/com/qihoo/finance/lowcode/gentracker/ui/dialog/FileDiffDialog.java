package com.qihoo.finance.lowcode.gentracker.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.SaveFile;
import com.qihoo.finance.lowcode.gentracker.ui.table.FileTableWrap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * FileDiffDialog
 *
 * @author fengjinfu-jk
 * date 2024/1/16
 * @version 1.0.0
 * @apiNote FileDiffDialog
 */
public class FileDiffDialog extends DialogWrapper {
    @Getter
    protected final Project project;
    protected final List<SaveFile> saveFiles;

    public FileDiffDialog(@NotNull Project project, @NotNull List<SaveFile> saveFiles) {
        super(project);
        this.project = project;
        this.saveFiles = saveFiles;

        // init
        initComponents();
        // setting components size
        settingSizes();
        // update status
        updateDialogStatus();
    }

    private void initComponents() {
        dialogPane = new JPanel(new BorderLayout());
        // title
        {
            JLabel title = new JLabel();
            title.setFont(new Font("微软雅黑", Font.BOLD, 13));
            title.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 14));
            title.setText("以下为生成文件列表, 请选择文件处理方式");
            title.setBorder(BorderFactory.createEmptyBorder(5, 5, 20, 5));

            dialogPane.add(title, BorderLayout.NORTH);
        }
        // file list
        {
            FileTableWrap fileTableWrap = new FileTableWrap(this, saveFiles);
            fileTable = fileTableWrap.createTable();
            dialogPane.add(new JBScrollPane(fileTable), BorderLayout.CENTER);
        }

        init();
        setModal(true);
        setTitle(GlobalDict.TITLE_INFO + " - 文件生成处理");
    }

    @Override
    protected void doOKAction() {
        if (oKButtonText.equals("全部覆盖") && checkMistake("全部覆盖", "确认覆盖所有文件？")) {
            return;
        }
        // 全部覆盖
        handlerFileList(true);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        if (cancelButtonText.equals("全部忽略") && checkMistake("全部忽略", "确认忽略所有文件？")) {
            return;
        }
        // 全部忽略
        ignoreFileList();
        super.doCancelAction();
    }

    private boolean checkMistake(String title, String warnMsg) {
        int check = Messages.showDialog("\n" + warnMsg, title, new String[]{"是", "否"}, 0, Icons.scaleToWidth(Icons.WARN, 60));
        return check != 0;
    }

    private void ignoreFileList() {
        SaveFile.SaveOptions saveOptions = new SaveFile.SaveOptions();
        for (int i = 0; i < fileTable.getModel().getRowCount(); i++) {
            SaveFile saveFile = (SaveFile) fileTable.getModel().getValueAt(i, FileTableWrap.FILE_IDX);
            saveFile.setIgnore(true);
        }
    }

    private void handlerFileList(boolean overwriteAll) {
        SaveFile.SaveOptions saveOptions = new SaveFile.SaveOptions();
        for (int i = 0; i < fileTable.getModel().getRowCount(); i++) {
            SaveFile saveFile = (SaveFile) fileTable.getModel().getValueAt(i, FileTableWrap.FILE_IDX);
            saveFile.write(overwriteAll, saveOptions);
        }
    }

    private void settingSizes() {
        JPanelUtils.setSize(this.dialogPane, new Dimension(700, 400));
    }

    public void updateDialogStatus() {
        oKButtonText = "全部覆盖";
        cancelButtonText = "全部忽略";
//        if (!FileTableWrap.existModify(fileTable)) {
//            oKButtonText = "完成";
//            cancelButtonText = "全部忽略";
//        } else {
//            oKButtonText = "全部覆盖";
//            cancelButtonText = "全部忽略";
//        }
        setOKButtonText(oKButtonText);
        setCancelButtonText(cancelButtonText);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }

    private JPanel dialogPane;
    private JBTable fileTable;
    private String oKButtonText;
    private String cancelButtonText;
}
