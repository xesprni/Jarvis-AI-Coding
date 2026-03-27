package com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.FormBuilder;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistoryType;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.common.util.UserUtils;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.ui.tree.MySQLTreePanel;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * ApiGroupDialog
 *
 * @author fengjinfu-jk
 * date 2023/9/22
 * @version 1.0.0
 * @apiNote ApiGroupDialog
 */
@Slf4j
public class SaveSQLDialog extends DialogWrapper {
    private final Project project;
    private final SQLExecuteHistory history;
    private final VirtualFile file;
    private final DatabaseNode database;
    private final String sql;
    private boolean reload = true;

    public SaveSQLDialog(@Nullable Project project, VirtualFile file, DatabaseNode database, String sql, SQLExecuteHistory history) {
        super(project);
        this.project = project;
        this.history = history;
        this.file = file;
        this.database = database;
        this.sql = sql;

        initComponents();

        // setting components size
        settingSizes();

        // setting components status
        settingComponentsStatus();

        // init data
        initEditData();

        log.info(Constants.Log.USER_ACTION, "用户打开SQL新增/编辑界面");
    }

    public void showBeforeClose() {
        reload = false;
        reloadTips.setVisible(true);
        show();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String text = sqlNameTextField.getText();
        if (StringUtils.isBlank(text)) {
            return new ValidationInfo("请填写SQL脚本名称", sqlNameTextField);
        }
        if (text.length() > 32) {
            return new ValidationInfo("SQL脚本名称长度最大为32字符", sqlNameTextField);
        }
        return super.doValidate();
    }

    private void settingSizes() {
        dialogPane.setPreferredSize(new Dimension(400, 60));
    }

    private void settingComponentsStatus() {
        // classNameTextField 只允许输入英文
        // ((AbstractDocument) sqlNameTextField.getDocument()).setDocumentFilter(DocumentUtils.createDocumentFilter(Constants.REGEX.ENG_NUM_UNDER_LINE));

        // 默认不允许提交
        // getOKAction().setEnabled(false);
    }

    @Override
    protected void doOKAction() {
        new SwingWorker<Result<List<SQLExecuteHistory>>, Result<List<SQLExecuteHistory>>>() {
            @Override
            protected Result<List<SQLExecuteHistory>> doInBackground() {
                progress.setVisible(true);
                // save sql history
                return saveSQLHistory();
            }

            @SneakyThrows
            @Override
            protected void done() {
                progress.setVisible(false);
                reload(get());
                super.done();
                close(OK_EXIT_CODE);
                NotifyUtils.notify("SQL语句保存成功", NotificationType.INFORMATION);
            }
        }.execute();
    }

    private Result<List<SQLExecuteHistory>> saveSQLHistory() {
        String datasourceType = database.getDataSourceType();
        SQLExecuteHistory data = new SQLExecuteHistory();
        if (Objects.nonNull(history)) {
            // id info
            BeanUtils.copyProperties(history, data);
        }

        data.setHistoryType(SQLExecuteHistoryType.USER_SAVE.getCode());
        data.setUserNo(UserUtils.getUserNo());
        data.setDataSource(database.getDataSourceType());
        data.setInstanceName(database.getInstanceName());
        data.setDatabaseName(database.getName());
        data.setSqlName(sqlNameTextField.getText());
        data.setSqlContent(sql);
        return DatabaseDesignUtils.saveSQLExecuteHistories(datasourceType, data);
    }

    private void reload(Result<List<SQLExecuteHistory>> saveResult) {
        if (saveResult.isSuccess() && CollectionUtils.isNotEmpty(saveResult.getData())) {
            List<SQLExecuteHistory> histories = saveResult.getData();
            // 保存成功后, 刷新历史记录接口缓存
            MySQLTreePanel sqlTreePanel = project.getService(MySQLTreePanel.class);
            sqlTreePanel.setSqlMenuItems(sqlTreePanel.buildSQLHistoryPopupMenu(histories));

            // virtual file 刷新
            if (reload && CollectionUtils.isNotEmpty(histories)) {
                SQLExecuteHistory currentSQL = histories.get(0);
                file.putUserData(SQLEditorManager.SQL_HISTORY, currentSQL);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        file.setWritable(true);
                        file.rename(this, currentSQL.getSqlName());
                        file.refresh(false, true);
                    } catch (IOException e) {
                        // ignore
                    } finally {
                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                        // 关闭文件
                        ApplicationManager.getApplication().invokeLater(() -> {
                            fileEditorManager.closeFile(file);
                            sqlTreePanel.openHistorySQLConsole(database, currentSQL);
                        });
                    }
                });
            }
            // 如果打开了历史记录Dialog, 同步reload历史记录Dialog
            SQLHistoryDialog.reloadIfShow();
        }
    }

    private void initEditData() {
        if (Objects.nonNull(history)) {
            this.sqlNameTextField.setText(history.getSqlName());
        } else {
            this.sqlNameTextField.setText(file.getName());
        }
    }

    private void initComponents() {
        reloadTips = new JLabel("SQL脚本文件内容已修改, 是否保存 ?");
        reloadTips.setForeground(JBColor.BLUE);
        reloadTips.setVisible(false);
        progress = JPanelUtils.createProgress();
        progress.setVisible(false);
        dialogPane = new JPanel(new BorderLayout());
        sqlNameTextField = new JTextField();
        JPanel content = FormBuilder.createFormBuilder()
                .addComponent(reloadTips)
                .addLabeledComponent("SQL脚本名称", sqlNameTextField)
                .addComponent(progress)
                .getPanel();

        dialogPane.add(content, BorderLayout.CENTER);

        init();
        setTitle(GlobalDict.TITLE_INFO + "-保存SQL脚本");
        setOKButtonText("保存");
        setCancelButtonText("取消");

    }

    private JPanel dialogPane;
    private JTextField sqlNameTextField;
    private JLabel reloadTips;
    private JProgressBar progress;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }

    public void showIfNew() {
        if (Objects.nonNull(history) && SQLExecuteHistoryType.USER_SAVE.getCode().equals(history.getHistoryType())) {
            // 如果已经保存过, 则直接执行保存
            doOKAction();
        } else {
            // 首次保存则提示命名
            show();
        }
    }
}
