package com.qihoo.finance.lowcode.apitrack.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.apitrack.listener.ApiTreeListener;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.DocumentUtils;
import com.qihoo.finance.lowcode.common.util.CacheManager;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

/**
 * ApiGroupDialog
 *
 * @author fengjinfu-jk
 * date 2023/9/22
 * @version 1.0.0
 * @apiNote ApiGroupDialog
 */
@Slf4j
public class ApiGroupDialog extends DialogWrapper {
    private final boolean isEdit;
    private final Project project;

    public ApiGroupDialog(@Nullable Project project, boolean isEdit) {
        super(project);
        this.project = project;
        this.isEdit = isEdit;

        initComponents();

        // setting components size
        settingSizes();

        // setting components status
        settingComponentsStatus();

        // init data
        initEditData();

        log.info(Constants.Log.USER_ACTION, "用户打开接口分类新增/编辑界面");
    }

    private void settingSizes() {
        dialogPane.setPreferredSize(new Dimension(500, 220));
    }

    private void settingComponentsStatus() {
        // classNameTextField 只允许输入英文
        ((AbstractDocument) classNameTextField.getDocument()).setDocumentFilter(DocumentUtils.createDocumentFilter(Constants.REGEX.ENG_NUM_UNDER_LINE));
    }

    @Override
    protected void doOKAction() {
        log.info(Constants.Log.USER_ACTION, "用户更新接口分类");

        try {
            ApplicationNode applicationNode = DataContext.getInstance(project).getSelectApplicationNode();
            if (!isEdit) {
                ApiGroupNode apiGroupNode = new ApiGroupNode();
                apiGroupNode.setProjectId(applicationNode.getProjectId());
                apiGroupNode.setProjectToken(applicationNode.getToken());
                apiGroupNode.setName(this.nameTextField.getText());
                apiGroupNode.setClassName(this.classNameTextField.getText());
                apiGroupNode.setClassDesc(this.descTextField.getText());

                ApiDesignUtils.apiCategoryAdd(applicationNode, apiGroupNode);
            } else {
                ApiGroupNode apiGroupNode = DataContext.getInstance(project).getSelectApiGroupNode();
                apiGroupNode.setName(this.nameTextField.getText());
                apiGroupNode.setClassName(this.classNameTextField.getText());
                apiGroupNode.setClassDesc(this.descTextField.getText());

                ApiDesignUtils.apiCategoryUpdate(applicationNode, apiGroupNode);
            }

            reload();
            super.doOKAction();
        } catch (Exception e) {
            Messages.showMessageDialog(e.getMessage(), (isEdit ? "编辑" : "新增") + "接口分类失败", Icons.scaleToWidth(Icons.FAIL, 60));
        }
    }

    private void reload() {
        CacheManager.refreshInnerCache();
        JTree tree = DataContext.getInstance(project).getApiTree();

        ApplicationNode applicationNode = DataContext.getInstance(project).getSelectApplicationNode();
        JTreeLoadingUtils.loading(true, tree, applicationNode, () -> {
            // 重新加载库
            return ApiDesignUtils.apiCategoryList(applicationNode);
        }, catList -> {
            ApiTreeListener.setSelectApiGroupNodes(project, applicationNode);
        });
    }

    private void initEditData() {
        ApplicationNode applicationNode = DataContext.getInstance(project).getSelectApplicationNode();
        this.prjTextField.setText(applicationNode.getName());

        if (!isEdit) return;
        ApiGroupNode apiGroupNode = DataContext.getInstance(project).getSelectApiGroupNode();
        this.nameTextField.setText(apiGroupNode.getName());
        this.descTextField.setText(apiGroupNode.getClassDesc());
        this.classNameTextField.setText(apiGroupNode.getClassName());
    }

    private void initComponents() {

        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        prjLabel = new JLabel();
        prjTextField = new JTextField();
        prjTextField.setEditable(false);

        nameLabel = new JLabel();
        nameTextField = new JTextField();

        classNameLabel = new JLabel();
        classNameTextField = new JTextField();

        descLabel = new JLabel();
        descTextField = new JTextField();

        //======== this ========
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        Dimension textFieldSize = new Dimension(400, 30);

        //======== dialogPane ========
        {
            dialogPane.setBorder(JBUI.Borders.empty(60, 60, 100, 60));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridLayoutManager(10, 2, JBUI.emptyInsets(), -1, -1));

                //---- label1 ----
                prjLabel.setText("所属应用");
                contentPanel.add(prjLabel, new GridConstraints(1, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                contentPanel.add(prjTextField, new GridConstraints(1, 1, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        textFieldSize, textFieldSize, textFieldSize));

                //---- label1 ----
                nameLabel.setText("接口分类");
                contentPanel.add(nameLabel, new GridConstraints(2, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                contentPanel.add(nameTextField, new GridConstraints(2, 1, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        textFieldSize, textFieldSize, textFieldSize));

                //---- label2 ----
                descLabel.setText("分类备注");
                contentPanel.add(descLabel, new GridConstraints(3, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                contentPanel.add(descTextField, new GridConstraints(3, 1, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        textFieldSize, textFieldSize, textFieldSize));

                //---- label2 ----
                classNameLabel.setText("Controller类名");
                contentPanel.add(classNameLabel, new GridConstraints(4, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                contentPanel.add(classNameTextField, new GridConstraints(4, 1, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        textFieldSize, textFieldSize, textFieldSize));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on

        init();
        setTitle(GlobalDict.TITLE_INFO + (isEdit ? "-编辑分类" : "-新增分类"));
        setOKButtonText("保存");
        setCancelButtonText("取消");
    }

    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel prjLabel;
    private JTextField prjTextField;
    private JLabel nameLabel;
    private JTextField nameTextField;
    private JLabel classNameLabel;
    private JTextField classNameTextField;
    private JLabel descLabel;
    private JTextField descTextField;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }
}
