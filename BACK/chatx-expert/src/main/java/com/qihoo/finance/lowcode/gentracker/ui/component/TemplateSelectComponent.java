package com.qihoo.finance.lowcode.gentracker.ui.component;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板选择组件
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class TemplateSelectComponent {
    public static final String DEFAULT_GROUP = "Default";

    @Getter
    private JPanel mainPanel;

    /**
     * 分组
     */
    @Getter
    private ComboBox<String> groupComboBox;

    /**
     * 选中所有复选框
     */
    @Getter
    private JBCheckBox allCheckbox;

    /**
     * 所有复选框
     */

    @Getter
    private List<JBCheckBox> checkBoxList;

    /**
     * 模板面板
     */
    private JPanel templatePanel;

    /**
     * 模板面板
     */
    private ActionListener checkBoxListener;

    private final boolean hiddenCheckBox;
    private final String datasource;
    private final Map<String, TemplateGroup> templateGroupMap;
    private static final Map<String, String> REPLACE_GROUP_CODE = new HashMap<>();

    static {
        REPLACE_GROUP_CODE.put("Default", "Mybatis");
        REPLACE_GROUP_CODE.put("Mybatis", "Default");
    }

    //------------------------------------------------------------------------------------------------------------------

    public TemplateSelectComponent(ActionListener checkBoxListener, boolean hiddenCheckBox, String datasource) {
        this.checkBoxListener = checkBoxListener;
        this.hiddenCheckBox = hiddenCheckBox;
        this.datasource = datasource;
        this.templateGroupMap = LowCodeAppUtils.queryTemplateGroup(datasource);
        this.init();
    }

    private void init() {
        this.mainPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());

        this.groupComboBox = new ComboBox<>();
        this.groupComboBox.setSwingPopup(false);
        this.groupComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupName = getSelectedGroupName();
                if (StringUtils.isEmpty(groupName)) {
                    return;
                }
                refreshTemplatePanel(groupName);
            }
        });
        this.allCheckbox = new JBCheckBox("全选");
        this.allCheckbox.setVisible(!hiddenCheckBox);
        this.allCheckbox.setPreferredSize(new Dimension(100, 30));
        this.allCheckbox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkBoxList == null) {
                    return;
                }
                for (JBCheckBox checkBox : checkBoxList) {
                    checkBox.setSelected(allCheckbox.isSelected());
                }

                checkBoxListener.actionPerformed(e);
            }
        });

        topPanel.add(this.groupComboBox, BorderLayout.CENTER);
        topPanel.add(this.allCheckbox, BorderLayout.EAST);
        this.mainPanel.add(topPanel, BorderLayout.NORTH);

        this.templatePanel = new JPanel(new GridLayout(-1, 3));
        this.mainPanel.add(templatePanel, BorderLayout.CENTER);

        this.refreshData();
    }

    private void refreshData() {
        this.groupComboBox.removeAllItems();
        for (TemplateGroup group : templateGroupMap.values()) {
            List<Template> templates = group.getElementList().stream().filter(template -> !template.isInnerTemplate()).collect(Collectors.toList());
            // 有剩下模板才使用
            if (CollectionUtils.isNotEmpty(templates)) {
                this.groupComboBox.addItem(replaceGroupCode(group.getName()));
            }
        }
    }

    private void refreshTemplatePanel(String groupName) {
        this.allCheckbox.setSelected(true);
        this.templatePanel.removeAll();
        this.checkBoxList = new ArrayList<>();
        TemplateGroup templateGroup = templateGroupMap.get(groupName);
        if (Objects.isNull(templateGroup)) return;

        for (Template template : templateGroup.getElementList()) {
            // skip inner template
            if (template.isInnerTemplate()) continue;

            JBCheckBox checkBox = new JBCheckBox(org.apache.commons.lang3.StringUtils.remove(template.getName(), ".vm"));
            checkBox.setSelected(true);
            checkBox.addActionListener(checkBoxListener);
            checkBox.setVisible(!this.hiddenCheckBox);

            this.checkBoxList.add(checkBox);
            this.templatePanel.add(checkBox);
        }
        this.mainPanel.updateUI();
    }

    public String getSelectedGroupName() {
        return replaceGroupCode((String) this.groupComboBox.getSelectedItem());
    }

    public boolean isDefaultGroup() {
        return DEFAULT_GROUP.equalsIgnoreCase(getSelectedGroupName());
    }

    public String replaceGroupCode(String groupCode) {
        return REPLACE_GROUP_CODE.getOrDefault(groupCode, groupCode);
    }

    public void setSelectedGroupName(String groupName) {
        this.groupComboBox.setSelectedItem(replaceGroupCode(groupName));
    }

    public List<Template> getAllTemplate() {
        String groupName = getSelectedGroupName();
        if (StringUtils.isEmpty(groupName)) {
            return Collections.emptyList();
        }

        TemplateGroup templateGroup = templateGroupMap.get(groupName);
        return templateGroup.getElementList();
    }

    public List<Template> getInnerTemplate(List<Template> selectTemplateList) {
        // 追加预置模板
        List<String> selectTemplateCodes = selectTemplateList.stream().map(Template::getName).collect(Collectors.toList());
        return getAllTemplate().stream().filter(Template::isInnerTemplate).filter(inner -> {
            String preResourceCode = inner.getPreResourceCode();
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(preResourceCode)) {
                // 存在前置模板要求, 需检查前置模板是否存在
                return (selectTemplateCodes.contains(preResourceCode));
            }
            return true;
        }).collect(Collectors.toList());
    }

    public List<Template> getAllSelectedTemplate() {
        String groupName = getSelectedGroupName();
        if (StringUtils.isEmpty(groupName)) {
            return Collections.emptyList();
        }
        TemplateGroup templateGroup = templateGroupMap.get(groupName);
        if (Objects.isNull(templateGroup)) return new ArrayList<>();

        Map<String, Template> map = templateGroup.getElementList().stream().collect(Collectors.toMap(Template::getName, val -> val));
        List<Template> result = new ArrayList<>();
        for (JBCheckBox checkBox : this.checkBoxList) {
            if (checkBox.isSelected()) {
                String templateName = checkBox.getText();
                if (!map.containsKey(templateName)) {
                    templateName = templateName.contains(".vm") ? templateName : templateName + ".vm";
                }

                Template template = map.get(templateName);
                if (template != null) {
                    result.add(template);
                }
            }
        }
        return result;
    }

    public void addActionListener(ActionListener listener) {
        this.groupComboBox.addActionListener(listener);
    }
}
