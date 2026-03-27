package com.qihoo.finance.lowcode.common.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.qifu.external.RipGrepUtil;
import com.qifu.utils.ConversationStore;
import com.qihoo.finance.lowcode.aiquestion.dto.GitIndex;
import com.qihoo.finance.lowcode.aiquestion.util.GitIndexUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.PluginConfig;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.enums.GitIndexStatus;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.declarative.action.DeclarativeSQLAction;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.editor.statusBar.EditorStatusBarWidget;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ChatxApplicationState;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qifu.devops.ide.plugins.jiracommit.listener.VcsChangeListListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ide启动，加载完index后，过几秒会执行该方法（不影响启动耗时）。
 */
@Slf4j
public class ChatxStartupActivity implements StartupActivity.Background {

    @Override
    @RequiresBackgroundThread
    public void runActivity(@NotNull Project project) {
        try {
            init(project);

            // 调用 toolWindow.getContentManager()，会触发 toolWindow 的初始化方法 createContentIfNeeded，
            StartupManager.getInstance(project).runAfterOpened(() -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    ToolWindow toolWindow = ChatXToolWindowFactory.getToolWindow();
                    if (toolWindow != null) {
                        toolWindow.getContentManager(); // ✅ 安全访问
                        log.info("ChatX ToolWindow initialized after project opened.");
                    } else {
                        log.warn("ChatX ToolWindow not yet available after project open.");
                    }
                });
            });
            // 索引代码仓库
            GitIndexUtils.buildIndex();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    GitIndex gitIndex = GitIndexUtils.gitIndexStatus(project);
                    if (gitIndex.getStatus().equals(GitIndexStatus.failed.name())
                            || gitIndex.getStatus().equals(GitIndexStatus.done.name())) {
                        return;
                    }
                    GitIndexUtils.flushGitIndexStatus(project);
                } catch (Exception ignored) {}
            }, 10, 10, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("ChatxStartupActivity got an exception", e);
        }
    }

    private static void init(Project project) {
        // config
        initConfig(project);
        // event
        initEvent(project);
        // install binary
        RipGrepUtil.install();
        dropOldVersionFile();
    }

    private static void dropOldVersionFile() {
        ConversationStore.deleteOldVersionsFile();
    }

    private static void initEvent(Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        manager.addChangeListListener(new VcsChangeListListener());
        log.info("ChatxStartupActivity initRegister register VcsChangeListListener");

        // LineMarkerAction
        // LineMarkerAction.registerAskAILineMarker();
        // log.info("ChatxStartupActivity initRegister register AILineMarker");
    }

    public static void initConfig(Project project) {
        boolean signedIn = ChatxService.getInstance().isSignedIn();
        Result<PluginConfig> pluginConfig = RestTemplateUtil.get(Constants.Url.GET_PLUGIN_CONFIG, null, null, new TypeReference<>() {});
        ApplicationManager.getApplication().invokeLater(() -> {
            pluginConfig(pluginConfig);
            askAiConfig(project, pluginConfig, signedIn);
        });

        // declarative sql
        DeclarativeSQLAction action = (DeclarativeSQLAction) ActionManager.getInstance().getAction(DeclarativeSQLAction.ACTION_ID);
        if (Objects.nonNull(action)) {
            String sqlPath = DatabaseDesignUtils.ddlExportPath();
            sqlPath = sqlPath.endsWith("/") ? sqlPath.substring(0, sqlPath.length() - 1) : sqlPath;
            action.setDeclarativeSQLFilePath(sqlPath);
        }
    }

    private static void pluginConfig(Result<PluginConfig> result) {
        if (result.isFail()) return;

        PluginConfig config = result.getData();
        ToolBarPanel toolBarPanel = ToolBarPanel.getInstance();
        toolBarPanel.setHelpDocHost(config.getHelpDocHost());
        toolBarPanel.setHomePageHost(config.getHomePageHost());
        toolBarPanel.setWebHost(config.getWebHost());
    }

    private static void askAiConfig(Project project, Result<PluginConfig> pluginConfigResult, boolean signedIn) {
        if (pluginConfigResult.isSuccess()) {
            log.info("plugin config loaded.");
            PluginConfig config = pluginConfigResult.getData();
            ChatxApplicationState settings = ChatxApplicationSettings.settings();
            settings.pluginName = config.getPluginName();
            settings.chatHelloText = config.getChatHelloText();
            settings.chatErrorMsg = config.getChatErrorMsg();
            settings.maxConversationCycle = config.getMaxConversationCycle();
            settings.maxConversationIdleSeconds = config.getMaxConversationIdleSeconds();
            settings.enabledCompletionDisableButton = config.isEnableCompletionDisableButton();
            Optional.ofNullable(config.getFreezeCompletionIntervals()).map(x -> x.split(","))
                    .map(x -> Arrays.stream(x).map(String::trim).map(Integer::valueOf).collect(Collectors.toList()))
                    .ifPresent(x -> settings.freezeCompletionIntervals = x);
            settings.unitTestPrefix = config.getUnitTestPrefix();
            settings.optimizeCodePrefix = config.getOptimizeCodePrefix();
            settings.explainCodePrefix = config.getExplainCodePrefix();
            settings.commentCodePrefix = config.getCommentCodePrefix();
            settings.disableCompletionAllowLanguageSet = new HashSet<>(Optional.ofNullable(config.getDisableCompletionAllowLanguages())
                    .map(s -> s.split(",")).map(Arrays::asList).orElseGet(Arrays::asList));
            settings.debounceMillis = Optional.ofNullable(config.getDebounceMillis()).orElse(0L);
            if (StringUtils.isNotBlank(config.getCompletionModel())) {
                settings.model = config.getCompletionModel();
            }
            if (!settings.enabledCompletionDisableButton) {
                settings.enableCompletions = true;
                if (signedIn) {
                    EditorStatusBarWidget.update(project, " " + ChatxBundle.get("chatx.completion.statusBar.enabled.text"));
                }
            }
            // 语法纠错不由服务端控制
//            if (config.getEnableSyntaxCorrection() != null) {
//                settings.setEnableSyntaxCorrection(config.getEnableSyntaxCorrection());
//            }
            if (config.getEnableMiddleCompletion() != null) {
                settings.enableMiddleCompletion = config.getEnableMiddleCompletion();
            }

//            QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
//            if (Objects.nonNull(questionPanel)) {
//                questionPanel.getInputPanelFactory().initResource(settings.assistants, settings.datasets, settings.shortcutInstructions);
//            }

            // translate
            if (StringUtils.isNotBlank(config.getTranslateModel())) {
                settings.translateModel = config.getTranslateModel();
            }
            if (StringUtils.isNotBlank(config.getTranslatePrompt())) {
                settings.translatePrompt = config.getTranslatePrompt();
            }

            // gitlab host
            if (StringUtils.isNotBlank(config.getGitlabHttpsHost())) {
                settings.gitlabHttpsHost = config.getGitlabHttpsHost();
            }
            if (StringUtils.isNotBlank(config.getGitlabSshHost())) {
                settings.gitlabSshHost = config.getGitlabSshHost();
            }
            settings.defaultJarvisModelId = config.getDefaultJarvisModelId();
            settings.oauth2ClientId = config.getOauth2ClientId();
            settings.oauth2ClientSecret = config.getOauth2ClientSecret();
            settings.skillTemplate = config.getSkillTemplate();
            settings.ruleTemplate = config.getRuleTemplate();
        }
        if (!signedIn) {
            ChatxStatusService.notifyApplication(ChatxStatus.NotSignedIn);
        }
    }
}
