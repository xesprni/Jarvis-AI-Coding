package com.qihoo.finance.lowcode.gentracker.ui.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ExceptionUtil;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfig;
import com.qihoo.finance.lowcode.gentracker.entity.GlobalConfigGroup;
import com.qihoo.finance.lowcode.gentracker.tool.CloneUtils;
import com.qihoo.finance.lowcode.gentracker.ui.component.EditListComponent;
import com.qihoo.finance.lowcode.gentracker.ui.component.EditorComponent;
import com.qihoo.finance.lowcode.gentracker.ui.component.GroupNameComponent;
import com.qihoo.finance.lowcode.gentracker.ui.component.LeftRightComponent;
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
 * 全局配置设置窗口
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Slf4j
public class GlobalConfigSettingForm implements Configurable, BaseSettings {
    /**
     * 全局变量描述信息，说明文档
     */
    private static final String TEMPLATE_DESCRIPTION_INFO;

    static {
        String descriptionInfo = "";
        try {
            URL url = Objects.requireNonNull(GlobalConfigSettingForm.class.getResource("/description/globalConfigDescription.html"));
            descriptionInfo = UrlUtil.loadText(url);
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
    private Map<String, GlobalConfigGroup> globalConfigGroupMap;
    /**
     * 当前分组名
     */
    private GlobalConfigGroup currGlobalConfigGroup;
    /**
     * 编辑框组件
     */
    private EditorComponent<GlobalConfig> editorComponent;
    /**
     * 分组操作组件
     */
    private GroupNameComponent<GlobalConfig, GlobalConfigGroup> groupNameComponent;
    /**
     * 编辑列表框
     */
    private EditListComponent<GlobalConfig> editListComponent;


    public GlobalConfigSettingForm() {
        this.mainPanel = new JPanel(new BorderLayout());
    }


    private void initGroupName() {
        Consumer<GlobalConfigGroup> switchGroupOperator = globalConfigGroup -> {
            this.currGlobalConfigGroup = globalConfigGroup;
            refreshUiVal();
            // 切换分组情况编辑框
            this.editorComponent.setFile(null);
        };

        this.groupNameComponent = new GroupNameComponent<>(switchGroupOperator, this.globalConfigGroupMap);
        this.mainPanel.add(groupNameComponent.getPanel(), BorderLayout.NORTH);
    }

    private void initEditList() {
        Consumer<GlobalConfig> switchItemFun = globalConfig -> {
            refreshUiVal();
            if (globalConfig != null) {
                this.editListComponent.setCurrentItem(globalConfig.getName());
            }
            editorComponent.setFile(globalConfig);
        };
        this.editListComponent = new EditListComponent<>(switchItemFun, "GlobalConfig Name:", GlobalConfig.class, this.currGlobalConfigGroup.getElementList());
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
        // 左右组件
        LeftRightComponent leftRightComponent = new LeftRightComponent(editListComponent.getMainPanel(), this.editorComponent.getMainPanel());
        this.mainPanel.add(leftRightComponent.getMainPanel(), BorderLayout.CENTER);
    }

    @Override
    public String getDisplayName() {
        return "Global Config";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public void loadSettingsStore(SettingsSettingStorage settingsStorage) {
        // 复制配置，防止篡改
        this.globalConfigGroupMap = CloneUtils.cloneByJson(settingsStorage.getGlobalConfigGroupMap(), new TypeReference<Map<String, GlobalConfigGroup>>() {
        });
        this.currGlobalConfigGroup = this.globalConfigGroupMap.get(settingsStorage.getCurrGlobalConfigGroupName());
        if (this.currGlobalConfigGroup == null) {
            this.currGlobalConfigGroup = this.globalConfigGroupMap.get(GlobalDict.DEFAULT_GROUP_NAME);
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
            log.error("ignore: {}", e.getMessage());
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !this.globalConfigGroupMap.equals(getSettingsStorage().getGlobalConfigGroupMap())
                || !getSettingsStorage().getCurrGlobalConfigGroupName().equals(this.currGlobalConfigGroup.getName());
    }

    @Override
    public void apply() {
        getSettingsStorage().setGlobalConfigGroupMap(this.globalConfigGroupMap);
        getSettingsStorage().setCurrGlobalConfigGroupName(this.currGlobalConfigGroup.getName());
        // 保存包后重新加载配置
        this.loadSettingsStore(getSettingsStorage());
    }

    private void refreshUiVal() {
        if (this.groupNameComponent != null) {
            this.groupNameComponent.setGroupMap(this.globalConfigGroupMap);
            this.groupNameComponent.setCurrGroupName(this.currGlobalConfigGroup.getName());
        }
        if (this.editListComponent != null) {
            this.editListComponent.setElementList(this.currGlobalConfigGroup.getElementList());
        }
    }
}
