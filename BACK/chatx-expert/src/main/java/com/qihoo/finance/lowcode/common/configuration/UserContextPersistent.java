package com.qihoo.finance.lowcode.common.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.qifu.devops.ide.plugins.jiracommit.domain.entity.ActiveJiraIssue;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 用户数据
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/10/16
 */
@State(name = "UserContext", storages = @Storage("userContext.xml"))
public class UserContextPersistent implements PersistentStateComponent<UserContextPersistent.UserContext> {
    private UserContext userContext = new UserContext();

    public static UserContextPersistent getInstance() {
        return ApplicationManager.getApplication().getService(UserContextPersistent.class);
    }

    @NotNull
    public static UserContextPersistent.UserContext getUserContext() {
        return getInstance().getState();
    }

    @NotNull
    @Override
    public UserContext getState() {
        return userContext;
    }

    @Override
    public void loadState(@NotNull UserContext state) {
        userContext = state;
    }

    @Data
    public static class UserContext {
        // 上次选择的模块, 队列, 先进先出, maxSize = 100;
        // 模块对应的包路径信息
        public Queue<String> selectModules = new ArrayDeque<>();
        public ActiveJiraIssue commitActiveJiraIssue;
        public String orderBy;
        public String exportDDLModule;

        public String entityAlias = "Entity";
        public String dtoAlias = "DTO";
        public String daoAlias = "DAO";
        public String mapperAlias = "Mapper";
        public String serviceAlias = "Service";
        public String controllerAlias = "Controller";
        public String facadeAlias = "Facade";

        public boolean syncEntityPackage = false;
        public boolean syncDtoPackage = false;
        public boolean syncDaoPackage = false;
        public boolean syncMapperPackage = false;
        public boolean syncServicePackage = false;
        public boolean syncControllerPackage = false;
        public boolean syncFacadePackage = false;

        public Map<String, List<String>> collects = new HashMap<>();

        public void addSelectModule(SelectModule module) {
            selectModules.add(JSON.toJson(module));
            if (selectModules.size() > 100) {
                selectModules.poll();
            }
        }

        public SelectModule peekModules(List<String> moduleNames) {
            List<String> reverseList = new ArrayList<>(selectModules);
            for (int size = reverseList.size() - 1; size >= 0; size--) {
                String json = reverseList.get(size);
                SelectModule module = JSON.parse(json, SelectModule.class);
                if (moduleNames.contains(module.getModuleName())) {
                    return module;
                }
            }

            return null;
        }

        public SelectModule peekModule(String moduleName) {
            List<String> reverseList = new ArrayList<>(selectModules);
            for (int size = reverseList.size() - 1; size >= 0; size--) {
                String json = reverseList.get(size);
                SelectModule module = JSON.parse(json, SelectModule.class);
                if (moduleName.equals(module.getModuleName())) {
                    return module;
                }
            }

            return null;
        }
    }
}
