package com.qihoo.finance.lowcode.aiquestion.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.CodeCompletionLogRequest;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.UpdateCodeCompletionLogStatusRequest;
import com.qihoo.finance.lowcode.common.entity.dto.askai.*;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.common.util.GitUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.editor.ChatxEditorManagerImpl;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.qihoo.finance.lowcode.common.constants.Constants.Url.*;

@Slf4j
public class ChatUtil extends LowCodeAppUtils {

    private final static Logger LOG = Logger.getInstance(ChatxEditorManagerImpl.class);

    private static Map<String, String> conversationIdMap = new HashMap<>();
    public static int conversationCycle = 0;
    /**
     * 上次请求的时间戳，单位秒
     */
    public static int lastConversationTimestamp = 0;
    // 请求超时时间，单位：毫秒
    public final static int READ_TIME_OUT = 60 * 1000;
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 60, TimeUnit.SECONDS
            , new LinkedBlockingQueue<>(100));

    @Deprecated
    public static String getConversationId(String assistant) {
        String conversationId = conversationIdMap.get(assistant);
        if (conversationId != null) {
            if (conversationCycle < ChatxApplicationSettings.settings().maxConversationCycle) {
                int conversationExpireTime = ChatxApplicationSettings.settings().maxConversationIdleSeconds
                        + lastConversationTimestamp;
                if (lastConversationTimestamp != 0 && conversationExpireTime <= System.currentTimeMillis() / 1000) {
                    // 如果会话过期了，重新开启一个会话
                    resetConversation(assistant);
                } else {
                    conversationCycle++;
                    lastConversationTimestamp = (int) (System.currentTimeMillis() / 1000);
                }
            } else {
                resetConversation(assistant);
            }
        }
        return conversationId;
    }

    @Deprecated
    public static void resetConversation(String assistant) {
        // dify sessionId不支持自定义
        conversationIdMap.remove(assistant);
        lastConversationTimestamp = 0;
        conversationCycle = 0;
    }

    @Deprecated
    public static void setConversationId(String assistant, String conversationId) {
        conversationIdMap.put(assistant, conversationId);
    }

    public static ChatCompletionOpenAIResponse chatCompletion(ChatCompletionOpenAIRequest request) {
        Result<ChatCompletionOpenAIResponse> result = RestTemplateUtil.post(CHAT_CHAT_COMPLETION_POST, request, new TypeReference<>() {
        });
        return result.isSuccess() ? result.getData() : ChatCompletionOpenAIResponse.empty();
    }

    public static CodeCompletionResponse codeCompletion(CodeCompletionRequest request) {
        try {
            Result<CodeCompletionResponse> result = RestTemplateUtil.post(CHAT_CODE_COMPLETION_V2, request
                    , new TypeReference<>() {
                    }, 60 * 1000);
            return result.getData();
        } catch (Exception e) {
            log.warn("code completion error", e);
            return null;
        }
    }

    public static void saveCodeCompletionLog(Editor editor, String prompt, CompletionType completionType
            , CompletionStatus completionStatus, String hintCode) {
        saveCodeCompletionLog(editor, null, prompt, completionType, completionStatus, hintCode, null);
    }

    public static void saveCodeCompletionLog(Editor editor, String uuid, String prompt, CompletionType completionType
            , CompletionStatus completionStatus, String hintCode, String model) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || !file.isInLocalFileSystem()) {
            LOG.info("File is not in local file system, ignore this code completion log.");
            return;
        }
        String filePath = file.getPath();
        if (editor.getProject() != null && editor.getProject().getBasePath() != null && filePath.startsWith(editor.getProject().getBasePath())) {
            filePath = filePath.substring(editor.getProject().getBasePath().length() + 1);
        }
        saveCodeCompletionLog(editor.getProject(), uuid, filePath, prompt, completionType, completionStatus, hintCode, model);
    }

    public static void saveCodeCompletionLog(Project project, String uuid, String filePath, String prompt, CompletionType completionType
            , CompletionStatus completionStatus, String hintCode, String model) {
        CodeCompletionLogRequest request = new CodeCompletionLogRequest();
        request.setUuid(uuid);
        request.setFilePath(filePath);
        request.setPrompt(prompt);
        request.setCompletionType(completionType);
        request.setHintCode(hintCode);
        request.setStatus(completionStatus.ordinal());
        request.setModel(model);
        executor.execute(() -> {
            String gitRepoUrl = GitUtils.getGitUrl(project);
            String branchName = GitUtils.getBranchName(project);
            request.setGitRepoUrl(gitRepoUrl);
            request.setBranchName(branchName);
            RestTemplateUtil.post(Constants.Url.CHAT_CODE_COMPLETION_LOG, request);
        });
    }

    public static void updateCodeCompletionLogStatus(String uuid, CompletionStatus status) {
        executor.execute(() -> {
            UpdateCodeCompletionLogStatusRequest request = new UpdateCodeCompletionLogStatusRequest();
            request.setUuid(uuid);
            request.setCompletionStatus(status);
            RestTemplateUtil.post(Constants.Url.CHAT_UPDATE_CODE_COMPLETION_LOG_STATUS, request);
        });
    }


    public static List<String> getSuggested(String messageId, QuestionType questionType) {
        QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
        if (Objects.nonNull(questionPanel)) {
            InputPanelFactory inputPanelFactory = questionPanel.getInputPanelFactory();
            String assistant = inputPanelFactory.getAssistant();
            String conversationId = inputPanelFactory.getConversationId();

            ChatCompletionRequest request = ChatCompletionRequest.of(messageId, conversationId, assistant, questionType);
            Result<List<String>> result = RestTemplateUtil.post(CHAT_GET_MESSAGE_SUGGESTED, request, new TypeReference<>() {
            });
            return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public static Map<String, String> getDatasets(QuestionType questionType) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionType", questionType);
        Result<Map<String, String>> result = RestTemplateUtil.get(CHAT_GET_DATASETS, data, new HashMap<>(), new TypeReference<>() {
        });
        return result.isSuccess() ? MapUtils.emptyIfNull(result.getData()) : new HashMap<>();
    }

    public static List<ChatConversationReponse> getConversations(String searchKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionType", QuestionType.ASK);
        if (StringUtils.isNotEmpty(searchKey)) {
            data.put("searchKey", searchKey);
        }
        Result<List<ChatConversationReponse>> result = RestTemplateUtil.get(CHAT_GET_CONVERSATIONS, data, new HashMap<>(), new TypeReference<>() {
        });
        return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
    }

    public static List<ChatMessageResponse> getConversationMessages(String assistantCode, String conversationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionType", QuestionType.ASK);
        data.put("conversationId", conversationId);
        data.put("assistantCode", assistantCode);
        Result<List<ChatMessageResponse>> result = RestTemplateUtil.get(CHAT_GET_CONVERSATION_MESSAGES, data, new HashMap<>(), new TypeReference<>() {
        });
        return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
    }

    public static boolean deleteConversation(String assistantCode, String conversationId) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionType", QuestionType.ASK);
        data.put("conversationId", conversationId);
        data.put("assistantCode", assistantCode);
        Result<List<ChatMessageResponse>> result = RestTemplateUtil.post(CHAT_POST_CONVERSATION_DELETE, data, new HashMap<>(), new TypeReference<>() {
        });
        return result.isSuccess();
    }

    public static void messageFeedbacks(String assistant, String messageId, ChatFeedbacksRequest.Rating rating) {
        if(StringUtils.isEmpty(assistant) || StringUtils.isEmpty(messageId)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("questionType", QuestionType.ASK);
        data.put("assistant", assistant);
        data.put("messageId", messageId);
        data.put("rating", rating);
        RestTemplateUtil.post(CHAT_POST_MESSAGE_FEEDBACKS, data, new HashMap<>(), new TypeReference<>() {});
    }

    public static List<DatasetInfo> getDatasets() {
        Result<List<DatasetInfo>> result = RestTemplateUtil.get(CHAT_GET_LIST_DATASET,
                new HashMap<>(), new HashMap<>(), new TypeReference<>() {
                });
        return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
    }

    public static List<ShortcutInstructionInfo> getInstructions() {
        Result<List<ShortcutInstructionInfo>> result = RestTemplateUtil.get(CHAT_GET_LIST_INSTRUCTION,
                new HashMap<>(), new HashMap<>(), new TypeReference<>() {
                });
        return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
    }

    public static List<AssistantInfo> getAssistants() {
        Result<List<AssistantInfo>> result = RestTemplateUtil.get(CHAT_GET_LIST_ASSISTANT,
                new HashMap<>(), new HashMap<>(), new TypeReference<>() {
                });
        return result.isSuccess() ? ListUtils.defaultIfNull(result.getData(), new ArrayList<>()) : new ArrayList<>();
    }
}
