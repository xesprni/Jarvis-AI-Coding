package com.qihoo.finance.lowcode.gentracker.ui.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * GitAuthorizationPanel
 *
 * @author fengjinfu-jk
 * date 2023/11/6
 * @version 1.0.0
 * @apiNote GitAuthorizationPanel
 */
public class GenOptionSettingForm implements Configurable {
    public static final String TITLE = "代码生成-参数配置";
    protected final UserContextPersistent userContextPersistent;
    protected final UserContextPersistent.UserContext userContext;

    public GenOptionSettingForm() {
        userContextPersistent = UserContextPersistent.getInstance();
        userContext = UserContextPersistent.getUserContext();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable JComponent createComponent() {
        initComponents();
        initComponentData();
        initComponentEvent();

        return mainPanel;
    }

    private void initComponentEvent() {
        DocumentAdapter showDemoLabelAction = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                showDemoLabel();
            }
        };

        entityAlias.getDocument().addDocumentListener(showDemoLabelAction);
        dtoAlias.getDocument().addDocumentListener(showDemoLabelAction);
        daoAlias.getDocument().addDocumentListener(showDemoLabelAction);
        mapperAlias.getDocument().addDocumentListener(showDemoLabelAction);
        serviceAlias.getDocument().addDocumentListener(showDemoLabelAction);
        controllerAlias.getDocument().addDocumentListener(showDemoLabelAction);
        facadeAlias.getDocument().addDocumentListener(showDemoLabelAction);

        syncEntityPackage.addActionListener(e -> showDemoLabel());
        syncDtoPackage.addActionListener(e -> showDemoLabel());
        syncDaoPackage.addActionListener(e -> showDemoLabel());
        syncMapperPackage.addActionListener(e -> showDemoLabel());
        syncServicePackage.addActionListener(e -> showDemoLabel());
        syncControllerPackage.addActionListener(e -> showDemoLabel());
        syncFacadePackage.addActionListener(e -> showDemoLabel());
    }

    private void initComponents() {
        mainPanel = new JPanel(new VFlowLayout(VFlowLayout.TOP));
        // add title
        mainPanel.add(initTitlePanel());
        // 文件尾缀
        entityAlias = new JTextField();
        dtoAlias = new JTextField();
        daoAlias = new JTextField();
        mapperAlias = new JTextField();
        serviceAlias = new JTextField();
        controllerAlias = new JTextField();
        facadeAlias = new JTextField();
        // 文件尾缀
        syncEntityPackage = new JBCheckBox("同步package");
        syncDtoPackage = new JBCheckBox("同步package");
        syncDaoPackage = new JBCheckBox("同步package");
        syncMapperPackage = new JBCheckBox("同步package");
        syncServicePackage = new JBCheckBox("同步package");
        syncControllerPackage = new JBCheckBox("同步package");
        syncFacadePackage = new JBCheckBox("同步package");

        entityLabel = new JLabel();
        dtoLabel = new JLabel();
        daoLabel = new JLabel();
        mapperLabel = new JLabel();
        serviceLabel = new JLabel();
        controllerLabel = new JLabel();
        facadeLabel = new JLabel();
        mainPanel.add(new TitledSeparator("默认生成类名配置"));
        GridLayout gridLayout = new GridLayout(-1, 2);

        JPanel suffixFormPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("entity"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, entityAlias, syncEntityPackage), entityLabel), 1, false)
                .addLabeledComponent(new JBLabel("dto"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, dtoAlias, syncDtoPackage), dtoLabel), 1, false)
                .addLabeledComponent(new JBLabel("dao"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, daoAlias, syncDaoPackage), daoLabel), 1, false)
                .addLabeledComponent(new JBLabel("mapper.xml"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, mapperAlias, syncMapperPackage), mapperLabel), false)
                .addLabeledComponent(new JBLabel("service"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, serviceAlias, syncServicePackage), serviceLabel), false)
                .addLabeledComponent(new JBLabel("controller"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, controllerAlias, syncControllerPackage), controllerLabel), false)
                .addLabeledComponent(new JBLabel("facade"), JPanelUtils.gridPanel(gridLayout, JPanelUtils.gridPanel(gridLayout, facadeAlias, syncFacadePackage), facadeLabel), false)
                .getPanel();
        mainPanel.add(suffixFormPanel);
    }

    public JPanel initTitlePanel() {
//        collapsible group header
        JPanel titlePanel = new JPanel(new FlowLayout());
        JLabel title = new JLabel();
        title.setIcon(Icons.scaleToWidth(Icons.TABLE_GEN_CODE, 20));
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setText(TITLE);
        titlePanel.add(title);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        return titlePanel;
    }

    private void initComponentData() {
        // 尝试从本地用户持久化信息中加载数据
        this.entityAlias.setText(StringUtils.defaultIfEmpty(userContext.entityAlias, "Entity"));
        this.dtoAlias.setText(StringUtils.defaultIfEmpty(userContext.dtoAlias, "DTO"));
        this.daoAlias.setText(StringUtils.defaultIfEmpty(userContext.daoAlias, "DAO"));
        this.mapperAlias.setText(StringUtils.defaultIfEmpty(userContext.mapperAlias, "Mapper"));
        this.serviceAlias.setText(StringUtils.defaultIfEmpty(userContext.serviceAlias, "Service"));
        this.controllerAlias.setText(StringUtils.defaultIfEmpty(userContext.controllerAlias, "Controller"));
        this.facadeAlias.setText(StringUtils.defaultIfEmpty(userContext.facadeAlias, "Facade"));

        this.syncEntityPackage.setSelected(userContext.syncEntityPackage);
        this.syncDtoPackage.setSelected(userContext.syncDtoPackage);
        this.syncDaoPackage.setSelected(userContext.syncDaoPackage);
        this.syncMapperPackage.setSelected(userContext.syncMapperPackage);
        this.syncServicePackage.setSelected(userContext.syncServicePackage);
        this.syncControllerPackage.setSelected(userContext.syncControllerPackage);
        this.syncFacadePackage.setSelected(userContext.syncFacadePackage);

        showDemoLabel();
    }

    private void showDemoLabel() {
        String entityPackage = syncEntityPackage.isSelected() ? StringUtils.lowerCase(entityAlias.getText()) : "entity";
        String dtoPackage = syncDtoPackage.isSelected() ? StringUtils.lowerCase(dtoAlias.getText()) : "dto";
        String daoPackage = syncDaoPackage.isSelected() ? StringUtils.lowerCase(daoAlias.getText()) : "dao";
        String mapperPackage = syncMapperPackage.isSelected() ? StringUtils.lowerCase(mapperAlias.getText()) : "mapper";
        String servicePackage = syncServicePackage.isSelected() ? StringUtils.lowerCase(serviceAlias.getText()) : "service";
        String controllerPackage = syncControllerPackage.isSelected() ? StringUtils.lowerCase(controllerAlias.getText()) : "controller";
        String facadePackage = syncFacadePackage.isSelected() ? StringUtils.lowerCase(facadeAlias.getText()) : "facade";

        this.entityLabel.setText(String.format("示例:  com.xx.domain.%s.Demo%s", entityPackage, StringUtils.capitalize(entityAlias.getText())));
        this.dtoLabel.setText(String.format("示例:  com.xx.%s.domain.Demo%s", dtoPackage, StringUtils.capitalize(dtoAlias.getText())));
        this.daoLabel.setText(String.format("示例:  com.xx.%s.Demo%s", daoPackage, StringUtils.capitalize(daoAlias.getText())));
        this.mapperLabel.setText(String.format("示例:  resources/%s/Demo%s.xml", mapperPackage, StringUtils.capitalize(mapperAlias.getText())));
        this.serviceLabel.setText(String.format("示例:  com.xx.%s.Demo%s", servicePackage, StringUtils.capitalize(serviceAlias.getText())));
        this.controllerLabel.setText(String.format("示例:  com.xx.%s.Demo%s", controllerPackage, StringUtils.capitalize(controllerAlias.getText())));
        this.facadeLabel.setText(String.format("示例:  com.xx.%s.Demo%s", facadePackage, StringUtils.capitalize(facadeAlias.getText())));
    }

    @Override
    public boolean isModified() {
        if (!this.entityAlias.getText().equals(userContext.entityAlias)) return true;
        if (!this.dtoAlias.getText().equals(userContext.dtoAlias)) return true;
        if (!this.daoAlias.getText().equals(userContext.daoAlias)) return true;
        if (!this.mapperAlias.getText().equals(userContext.mapperAlias)) return true;
        if (!this.serviceAlias.getText().equals(userContext.serviceAlias)) return true;
        if (!this.controllerAlias.getText().equals(userContext.controllerAlias)) return true;
        if (!this.facadeAlias.getText().equals(userContext.facadeAlias)) return true;

        if (this.syncEntityPackage.isSelected() != userContext.syncEntityPackage) return true;
        if (this.syncDtoPackage.isSelected() != userContext.syncDtoPackage) return true;
        if (this.syncDaoPackage.isSelected() != userContext.syncDaoPackage) return true;
        if (this.syncMapperPackage.isSelected() != userContext.syncMapperPackage) return true;
        if (this.syncServicePackage.isSelected() != userContext.syncServicePackage) return true;
        if (this.syncControllerPackage.isSelected() != userContext.syncControllerPackage) return true;
        if (this.syncFacadePackage.isSelected() != userContext.syncFacadePackage) return true;

        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        // 持久化到本地用户信息中
        userContext.entityAlias = this.entityAlias.getText();
        userContext.dtoAlias = this.dtoAlias.getText();
        userContext.daoAlias = this.daoAlias.getText();
        userContext.mapperAlias = this.mapperAlias.getText();
        userContext.serviceAlias = this.serviceAlias.getText();
        userContext.controllerAlias = this.controllerAlias.getText();
        userContext.facadeAlias = this.facadeAlias.getText();

        userContext.syncEntityPackage = this.syncEntityPackage.isSelected();
        userContext.syncDtoPackage = this.syncDtoPackage.isSelected();
        userContext.syncDaoPackage = this.syncDaoPackage.isSelected();
        userContext.syncMapperPackage = this.syncMapperPackage.isSelected();
        userContext.syncServicePackage = this.syncServicePackage.isSelected();
        userContext.syncControllerPackage = this.syncControllerPackage.isSelected();
        userContext.syncFacadePackage = this.syncFacadePackage.isSelected();

        userContextPersistent.loadState(userContext);
    }

    //------------------------------------------------------------------------------------------------------------------

    private JPanel mainPanel;
    private JTextField entityAlias;
    private JTextField dtoAlias;
    private JTextField daoAlias;
    private JTextField mapperAlias;
    private JTextField serviceAlias;
    private JTextField controllerAlias;
    private JTextField facadeAlias;

    private JLabel entityLabel;
    private JLabel dtoLabel;
    private JLabel daoLabel;
    private JLabel mapperLabel;
    private JLabel serviceLabel;
    private JLabel controllerLabel;
    private JLabel facadeLabel;

    private JBCheckBox syncEntityPackage;
    private JBCheckBox syncDtoPackage;
    private JBCheckBox syncDaoPackage;
    private JBCheckBox syncMapperPackage;
    private JBCheckBox syncServicePackage;
    private JBCheckBox syncControllerPackage;
    private JBCheckBox syncFacadePackage;
}
