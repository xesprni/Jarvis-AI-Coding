package com.qihoo.finance.lowcode.gentracker.ui.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ExceptionUtil;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.tool.CloneUtils;
import com.qihoo.finance.lowcode.gentracker.ui.component.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 生成模板设置窗口
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class TemplateSettingForm implements Configurable, BaseSettings {
    /**
     * 模板描述信息，说明文档
     */
    private static final String TEMPLATE_DESCRIPTION_INFO;

    static {
        String descriptionInfo = "";
        try {
            URL resource = TemplateSettingForm.class.getResource("/description/templateDescription.html");
            descriptionInfo = UrlUtil.loadText(Objects.requireNonNull(resource));
        } catch (IOException e) {
            ExceptionUtil.rethrow(e);
        } finally {
            TEMPLATE_DESCRIPTION_INFO = descriptionInfo;
        }
    }

    private final JPanel mainPanel;
    /**
     * 类型映射配置
     */
    private Map<String, TemplateGroup> templateGroupMap;
    /**
     * 当前分组名
     */
    private TemplateGroup currTemplateGroup;
    /**
     * 编辑框组件
     */
    private EditorComponent<Template> editorComponent;
    /**
     * 分组操作组件
     */
    private GroupNameComponent<Template, TemplateGroup> groupNameComponent;
    /**
     * 编辑列表框
     */
    private EditListComponent<Template> editListComponent;


    public TemplateSettingForm() {
        this.mainPanel = new JPanel(new BorderLayout());
    }


    private void initGroupName() {
        Consumer<TemplateGroup> switchGroupOperator = templateGroup -> {
            this.currTemplateGroup = templateGroup;
            refreshUiVal();
            // 切换分组情况编辑框
            this.editorComponent.setFile(null);
        };

        this.groupNameComponent = new GroupNameComponent<>(switchGroupOperator, this.templateGroupMap);
        this.mainPanel.add(groupNameComponent.getPanel(), BorderLayout.NORTH);
    }

    private void initEditList() {
        Consumer<Template> switchItemFun = template -> {
            refreshUiVal();
            if (template != null) {
                this.editListComponent.setCurrentItem(template.getName());
            }
            editorComponent.setFile(template);
        };
        this.editListComponent = new EditListComponent<>(switchItemFun, "Template Name:", Template.class, this.currTemplateGroup.getElementList());
    }

    private void initEditor() {
        this.editorComponent = new EditorComponent<>(null, TEMPLATE_DESCRIPTION_INFO);
    }

    private void initPanel() {
        this.loadSettingsStore(getSettingsStorage());
        // 初始化表格
        this.initGroupName();
        // 初始化编辑列表组件
        this.initEditList();
        // 初始化编辑框组件
        this.initEditor();
        // 初始化实时调试
        this.initRealtimeDebug();
        // 左右组件
        LeftRightComponent leftRightComponent = new LeftRightComponent(editListComponent.getMainPanel(), this.editorComponent.getMainPanel());
        this.mainPanel.add(leftRightComponent.getMainPanel(), BorderLayout.CENTER);
    }

    private void initRealtimeDebug() {
        RealtimeDebugComponent realtimeDebugComponent = new RealtimeDebugComponent(editorComponent);
        groupNameComponent.getPanel().add(realtimeDebugComponent.getMainPanel());
    }

    @Override
    public String getDisplayName() {
        return "代码生成-模板调试";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public void loadSettingsStore(SettingsSettingStorage settingsStorage) {
        // 复制配置，防止篡改
        this.templateGroupMap = CloneUtils.cloneByJson(settingsStorage.getTemplateGroupMap(), new TypeReference<Map<String, TemplateGroup>>() {
        });
        this.currTemplateGroup = this.templateGroupMap.get(settingsStorage.getCurrTemplateGroupName());
        if (this.currTemplateGroup == null) {
            this.currTemplateGroup = this.templateGroupMap.get(GlobalDict.DEFAULT_GROUP_NAME);
        }
        // 解决reset后编辑框未清空BUG
        if (this.editorComponent != null) {
            this.editorComponent.setFile(null);
        }
        this.refreshUiVal();
    }

    @Override
    public @Nullable JComponent createComponent() {
        try {
            this.initPanel();
        } catch (Exception e) {
            // ignore
            log.error(e.getMessage());
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !this.templateGroupMap.equals(getSettingsStorage().getTemplateGroupMap())
                || !getSettingsStorage().getCurrTemplateGroupName().equals(this.currTemplateGroup.getName());
    }

    @Override
    public void apply() {
        getSettingsStorage().setTemplateGroupMap(this.templateGroupMap);
        getSettingsStorage().setCurrTemplateGroupName(this.currTemplateGroup.getName());
        // 保存包后重新加载配置
        this.loadSettingsStore(getSettingsStorage());
    }

    private void refreshUiVal() {
        if (this.groupNameComponent != null) {
            this.groupNameComponent.setGroupMap(this.templateGroupMap);
            this.groupNameComponent.setCurrGroupName(this.currTemplateGroup.getName());
        }
        if (this.editListComponent != null) {
            this.editListComponent.setElementList(this.currTemplateGroup.getElementList());
        }
    }
}
