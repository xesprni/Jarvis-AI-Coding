package com.qihoo.finance.lowcode.settings;

import com.intellij.lang.Language;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ShortcutInstructionInfo;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class ChatxApplicationState {

    @OptionTag("maxTokens")
    public int maxTokens = 500;

    @OptionTag("candidateNum")
    public String candidateNum = "1";

    @OptionTag("temperature")
    public String temperature = "0.2";

    @OptionTag("topK")
    public String topK = "0";

    @OptionTag("topP")
    public String topP = "1.0";

    @OptionTag("askCodegeexLanguageSettingEnum")
    public String askCodegeexLanguageSettingEnum = "Default";

    @OptionTag("generateCommentLanguageSettingEnum")
    public String generateCommentLanguageSettingEnum = "Default";

    @OptionTag("explainCodeLanguageSettingEnum")
    public String explainCodeLanguageSettingEnum = "Default";

    @OptionTag("askCodegeexThemeEnum")
    public String askCodegeexThemeEnum = "sync";

    @OptionTag("generatePreferenceEnum")
    public String generatePreferenceEnum = "Automatic";

    @OptionTag("telemetryEnabled")
    public boolean telemetryEnabled = false;

    @OptionTag("isTriggerByKey")
    public boolean isTriggerByKey = false;

    @OptionTag("telemetryConfigured")
    public boolean telemetryConfigured = false;

    @OptionTag("multilineCompletionCount")
    public int multilineCompletionCount = 3;

    @OptionTag("signinNotificationShown")
    public boolean signinNotificationShown = false;

    @OptionTag("enableCompletions")
    public boolean enableCompletions = true;
//    public boolean enableCompletions = false;

    @XCollection(style = XCollection.Style.v2, propertyElementName = "disabledLanguages", elementTypes = {String.class})
    private final Set<String> disabledLanguageIds = new HashSet<>();

    @Nullable
    @OptionTag(value = "inlayTextColor", converter = ColorConverter.class)
    public Color inlayTextColor = null;

    @Getter
    @OptionTag("showIdeCompletions")
    private boolean showIdeCompletions = false;

    @OptionTag("checkForUpdate")
    public boolean checkForUpdate = true;

    @OptionTag("disableHttpCache")
    public transient boolean internalDisableHttpCache = false;

    public String conversationId = "";

    public String sessionId = "";

    public String userId = "";

    public String userName = "";

    public String email = "";

    public String source = "";

    public String sourceId = "";

    public boolean requestLimitNotificationShown = false;

    public Integer configCacheSeconds = Integer.valueOf(60);

    public Integer minCompletionDelay = Integer.valueOf(100);

    public String pluginName = GlobalDict.PLUGIN_NAME;
    public String chatHelloText;
    public String chatErrorMsg;
    @Transient
    public String minVersion;
    public Integer maxConversationCycle;
    public Integer maxConversationIdleSeconds;

    @Transient
    public boolean enabledCompletionDisableButton;
    @Transient
    public boolean enableSyntaxCorrection = false;
    @Transient
    public boolean enableMiddleCompletion = true;
    @Transient
    public String defaultJarvisModelId = "qwen3-coder";
    // 聊天窗口使用的modelId, ModelConfig类的Id
    public String chatModelId = null;
    public Boolean modelSupportImage = false;

    public String unitTestPrefix = "请帮忙生成单元测试代码，代码如下：\n";
    public String translatePrefix = "请帮忙优化代码变量命名，代码如下：\n";
    public String explainCodePrefix = "请帮忙解释代码含义，代码如下：\n";
    public String optimizeCodePrefix = "请帮忙优化代码，代码如下：\n";
    public String commentCodePrefix = "请帮忙生成代码注释，代码如下：\n";
    public String translatePrompt = "你是一位翻译官，请把用户输入转换为驼峰式英文和下划线式英文，如果用户输入的英文是，同时返回对应的中文翻译。结果直接以','间隔返回，不需要包含单双引号，比如'appBaseInfo,app_base_info,应用基础信息'\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"Question: appCode\\n\" +\n" +
            "                \"Answer: appCode,app_code,应用编码\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"Question: RESPONSE_ENTITY\\n\" +\n" +
            "                \"Answer: responseEntity,response_entity,响应实体\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"Question: 是否绑定\\n\" +\n" +
            "                \"Answer: isBound,is_bound\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"\\n\" +\n" +
            "                \"Question: 是否校验\\n\" +\n" +
            "                \"Answer: isCheck,is_check";
    public String translateModel = "JARVIS_TURBO";


    // 缓存最大trim的字符数量，用户输入的字符与提示的字符相同数量超过后，将取消这个cache，并禁用一段时间的提示
    @Transient
    @Deprecated
    public int cacheMaxTrimPrefixLength = 5;
    @Transient
    public int disableCompletionIntervalSeconds = 30;
    @Transient
    // 获取补全必须大于的时间
    public long fetchCompletionAfter = 0L;
    /**
     * 可以禁用补全的参数列表
     */
    public Set<String> disableCompletionAllowLanguageSet = new HashSet<>();

    public String lineMode = "server";
    /**
     * 智能模式使用的补全模式
     */
    @Transient
    public CompletionMode completionMode = CompletionMode.ONE_STATEMENT;
    /** 连续应用的次数 */
    @Transient
    public int consecutiveApplyCount = 0;
    /** 连续取消应用的次数 */
    @Transient
    public int consecutiveRejectCount = 0;
    @Transient
    public List<Integer> freezeCompletionIntervals = Arrays.asList(10, 10, 20, 30, 50, 80, 130, 210);
    public long lastFetchCompletionTime = 0;
    /** 补全防抖时间 */
    @Transient
    public long debounceMillis = 0;

    public String model = "codegeex-lite";
    public String dataset = "";
    public boolean withDataset = true;
    public List<DatasetInfo> datasets = new ArrayList<>();
    public List<AssistantInfo> assistants = new ArrayList<>();
    public List<ShortcutInstructionInfo> shortcutInstructions = new ArrayList<>();

    public String gitlabHttpsHost = "https://gitlab.daikuan.qihoo.net";
    public String gitlabSshHost = "ssh://git@gitlab.daikuan.qihoo.net:2222";
    public String oauth2ClientId = "";
    public String oauth2ClientSecret = "";
    public String skillTemplate = "";
    public String ruleTemplate = "";


    public boolean isEnabled(@NotNull Language language) {
        return !this.disabledLanguageIds.contains(language.getID());
    }

    public void enableLanguage(@NotNull Language language) {
        this.disabledLanguageIds.remove(language.getID());
    }

    public void disableLanguage(@NotNull Language language) {
        this.disabledLanguageIds.add(language.getID());
    }

    public AssistantInfo getAssistantInfo(String assistantCode) {
        Map<String, AssistantInfo> assistantMap = assistants.stream().collect(Collectors.toMap(AssistantInfo::getCode, Function.identity()));
        if (assistantMap.get(assistantCode) == null) {
            if (Constants.DEFAULT_ASSISTANT.equals(assistantCode)) {
                AssistantInfo assistantInfo = new AssistantInfo();
                assistantInfo.setCode(assistantCode);
                assistantInfo.setName(Constants.DEFAULT_ASSISTANT_NAME);
                return assistantInfo;
            }
        }
        return assistantMap.get(assistantCode);
    }

    public List<DatasetInfo> getDatasets(String assistantCode) {
        return datasets.stream().filter(datasetInfo -> assistantCode.equals(datasetInfo.getAssistantCode()))
                .collect(Collectors.toList());
    }

    public List<ShortcutInstructionInfo> getInputInstructions(String assistantCode) {
        // 助手绑定快捷指令, 无 assistantCode 表示通用快捷指令, 所有助手均可访问
        return shortcutInstructions.stream()
                .filter(s -> s.isInstruction(ShortcutInstructionInfo.InstructionType.INPUT_INSTRUCTION))
                .filter(instruction ->
                        StringUtils.isBlank(instruction.getAssistantCode()) || assistantCode.equals(instruction.getAssistantCode())
                ).collect(Collectors.toList());
    }
}
