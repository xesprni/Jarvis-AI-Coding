package com.qihoo.finance.lowcode.common.entity;

import lombok.Data;

/**
 * PluginConfig
 *
 * @author fengjinfu-jk
 * date 2023/10/20
 * @version 1.0.0
 * @apiNote PluginConfis
 */
@Data
public class PluginConfig {
    private String gitlabHttpsHost = "https://gitlab.daikuan.qihoo.net";
    private String gitlabSshHost = "ssh://git@gitlab.daikuan.qihoo.net:2222";
    private String pluginHost = "http://artifacts.daikuan.qihoo.net/artifacts/public/plugins/chatx-expert/updatePlugins.xml";
    private String pluginHost2 = "http://artifacts.daikuan.qihoo.net/artifacts/public/plugins/chatx-expert/updatePlugins2.xml";
    private String helpDocHost = "https://docs.daikuan.qihoo.net/lowcode/";
    private String homePageHost = "https://lowcode.daikuan.qihoo.net/";
    private String webHost = "https://jarvis.daikuan.qihoo.net";
    /**
     * 强制升级
     */
    private boolean forceUpdate;
    /**
     * 禁用数据库设计
     */
    private boolean disableDbDesign;
    /**
     * 禁用数据库代码生成
     */
    private boolean disableDbGenerate;
    /**
     * 禁用API设计
     */
    private boolean disableApiDesign;
    /**
     * 禁用API代码生成
     */
    private boolean disableApiGenerate;
    /**
     * 禁用代码审查
     */
    private boolean disableCodeReview;
    /**
     * 禁用AI问答
     */
    private boolean disableAiQuestion;

    private String pluginName;
    private String chatHelloText;
    private String chatErrorMsg;
    private Integer maxConversationCycle;
    private Integer maxConversationIdleSeconds;
    private boolean enableCompletionDisableButton;
    private String minVersion;
    private Boolean enableSyntaxCorrection;
    private Boolean enableMiddleCompletion;

    private String explainCodePrefix;
    private String optimizeCodePrefix;
    private String unitTestPrefix;
    private String commentCodePrefix;
    private String disableCompletionAllowLanguages;
    /** 代码补全使用的模型 */
    private String completionModel;
    private String freezeCompletionIntervals;
    /** 代码补全防抖时长 */
    private Long debounceMillis;
//    private String assistantConfigStr;
//    private String shortcutInstructionStr;
    /** 知识库配置 */
//    private String datasetConfigStr;

    public String translatePrompt;
    public String translateModel;

    /** 默认聊天 Jarvis模型Id, 对应数据库jarvis_model_info表的jarvis_model_id字段 */
    public String defaultJarvisModelId;

    public String oauth2ClientId;
    public String oauth2ClientSecret;

    public String skillTemplate;
    public String ruleTemplate;
}
