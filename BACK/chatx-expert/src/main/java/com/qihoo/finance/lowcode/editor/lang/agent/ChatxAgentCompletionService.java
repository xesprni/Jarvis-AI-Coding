package com.qihoo.finance.lowcode.editor.lang.agent;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CodeCompletionRequest;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CodeCompletionResponse;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.common.util.ChatxBundle;
import com.qihoo.finance.lowcode.common.util.FileSizeUtil;
import com.qihoo.finance.lowcode.editor.ChatxDefaultInlayRenderer;
import com.qihoo.finance.lowcode.editor.ChatxService;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletion;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletionService;
import com.qihoo.finance.lowcode.editor.completions.ChatxEditorInlay;
import com.qihoo.finance.lowcode.editor.completions.ChatxInlayList;
import com.qihoo.finance.lowcode.editor.completions.CompletionCache;
import com.qihoo.finance.lowcode.editor.completions.CompletionUtil;
import com.qihoo.finance.lowcode.editor.completions.SimpleCompletionCache;
import com.qihoo.finance.lowcode.editor.lang.TypingAsSuggestedCompletionUtil;
import com.qihoo.finance.lowcode.editor.lang.agent.commands.GetCompletionsResult;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.telemetry.LogTelemetries;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ChatxApplicationState;
import com.qihoo.finance.lowcode.status.ChatxStatus;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class ChatxAgentCompletionService implements ChatxCompletionService, Disposable {

    private static final Logger LOG = Logger.getInstance(ChatxAgentCompletionService.class);

    protected final CompletionCache cache = new SimpleCompletionCache(32);

    private String lang;
    private ChatxStatus lastStatusBarStatus;
    LogTelemetries logTelemetries = new LogTelemetries();

    public boolean isAvailable(@NotNull Editor editor) {
        Project project = editor.getProject();
        if (project == null) {
            return false;
        }
        this.lang = ChatxService.getInstance().getLanguageFromEditor(editor);
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        return file != null && !(file instanceof com.intellij.psi.PsiBinaryFile)
                && !file.getFileType().isBinary()
                && FileSizeUtil.isSupported(file.getVirtualFile());
    }
    @Override
    public @Nullable EditorRequest createRequest(@NotNull Editor editor, int offset
            , @NotNull CompletionType completionType) {
        return AgentEditorRequest.create(editor, offset, completionType);
    }

    @Override
    public boolean fetchCompletions(@NotNull EditorRequest request, @Nullable Integer maxCompletions, boolean cycling
            , Flow.@NotNull Subscriber<List<ChatxInlayList>> subscriber, Editor editor) {
        ApplicationManager.getApplication().runReadAction(() -> {
            request.initPromptFromContext();
            this.lang = request.getLang();
        });
        if (StringUtils.isBlank(request.getPrompt())) {
            LOG.info("No prompt found, skipping completions");
            return false;
        }
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        CodeCompletionRequest param = new CodeCompletionRequest();
        param.setLang(lang);
        param.setPrompt(request.getPrompt());
        param.setPromptElementRanges(request.getPromptElementRanges());
        param.setSuffix(request.getSuffix());
        param.setTabWidth(request.getTabWidth());
        param.setTopK(Double.valueOf(settings.topK));
        param.setTopP(Double.valueOf(settings.topP));
        param.setLineMode(settings.lineMode);
        param.setCompletionMode(request.getCompletionMode());
        param.setModel(settings.model);
        param.setTemperature(Double.valueOf(settings.temperature));
        param.setCompletionType(request.getCompletionType());
        param.setIsFimEnabled(!request.getLineInfo().getLineSuffix().isBlank());
        if (ChatxStatus.CompletionInProgress != ChatxStatusService.getCurrentStatus().getFirst()) {
            lastStatusBarStatus = ChatxStatusService.getCurrentStatus().getFirst();
        }
        ChatxStatusService.notifyApplication(ChatxStatus.CompletionInProgress, "");
        CodeCompletionResponse result = ChatUtil.codeCompletion(param);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completion request: \n" + param + "\nresponse:\n" + result);
        }
        if (result == null || ArrayUtils.isEmpty(result.getCode()) || result.getCode()[0].isEmpty()) {
            ChatxStatusService.notifyApplication(lastStatusBarStatus, " "
                    + ChatxBundle.get("chatx.completion.statusBar.nosuggestion.text"));
            return false;
        }
        try (SubmissionPublisher<List<ChatxInlayList>> publisher = new SubmissionPublisher<>();) {
            publisher.subscribe(subscriber);
            ArrayList<ChatxInlayList> inlayLists = new ArrayList<>();
            for (String completion : result.getCode()) {
                List<String> lines = ChatxDefaultInlayRenderer.replaceLeadingTabs(List.of(ChatxStringUtil.splitLines(completion)), request);
//                lines = CompletionUtil.reduceCompletionLine(request, lines);
                if (lines.isEmpty()) {
                    ChatxStatusService.notifyApplication(lastStatusBarStatus, " "
                            + ChatxBundle.get("chatx.completion.statusBar.nosuggestion.text"));
                    return false;
                }
                AgentCompletion agentCompletion = AgentCompletion.fromString(UUID.randomUUID().toString().replaceAll("\\-", "")
                        , StringUtils.join(lines, "\n"), result.getCompletionMode(), result.getModel());
                String docPrefix = request.getCurrentDocumentPrefix();
                String prefixTrimmed = CompletionUtil.TrimEndSpaceTab(docPrefix);
                cache.add(prefixTrimmed, prefixTrimmed, true, agentCompletion);
                ChatxInlayList inlays = CompletionUtil.createEditorCompletion(request, agentCompletion, true);
                if (inlays != null) {
                    inlayLists.add(inlays);
                }
            }
            if (!inlayLists.isEmpty()) {
                publisher.submit(inlayLists);
            }
        }
        ChatxStatusService.notifyApplication(lastStatusBarStatus, " "
                + ChatxBundle.get("chatx.completion.statusBar.done.text"));
        return true;
    }

    @Override
    public @Nullable List<ChatxInlayList> fetchCachedCompletions(@NotNull EditorRequest request) {
        return TypingAsSuggestedCompletionUtil.handleTypeAheadCaching(request, this.cache);
    }

    @Override
    public void sendShownTelemetry(EditorRequest request, @NotNull ChatxCompletion chatxCompletion, Editor editor) {
        GetCompletionsResult.Completion agentData = ((AgentCompletion) chatxCompletion).getAgentData();
        if (agentData.getCompletionStatus() != null) {
            LOG.debug("Sending shown telemetry skipped, completion status:" + agentData.getCompletionStatus());
            return;
        }
        agentData.setCompletionStatus(CompletionStatus.SHOWN);
        String hintCode = agentData.getText();
        String uuid = agentData.getUuid();
        String documentContent = request.getDocumentContent();
        String prompt = documentContent.substring(0, request.getOffset());
        String promptTrimmedEnd = CompletionUtil.TrimEndSpaceTab(prompt);
        ChatUtil.saveCodeCompletionLog(editor, uuid, promptTrimmedEnd, CompletionType.GHOST_TEXT, CompletionStatus.SHOWN
                , hintCode, agentData.getModel());
    }

    @Override
    public LogTelemetries getLogTelemetries() {
        return logTelemetries;
    }

    @Override
    public void sendAcceptedTelemetry(@NotNull ChatxCompletion chatxCompletion) {
        GetCompletionsResult.Completion agentData = ((AgentCompletion) chatxCompletion).getAgentData();
        if (agentData.getCompletionStatus() != CompletionStatus.SHOWN) {
            LOG.debug("Chatx completion was not in shown status, not sending accepted telemetry, current status"
                    + agentData.getCompletionStatus());
            return;
        }
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        settings.consecutiveApplyCount++;
        settings.consecutiveRejectCount = 0;
        if ("server".equals(settings.lineMode) && settings.consecutiveApplyCount > 1) {
            if (settings.completionMode == CompletionMode.ONE_LINE) {
                settings.completionMode = CompletionMode.ONE_STATEMENT;
                LOG.debug("Chatx completion mode was changed from one line to one statement");
            } else if (settings.completionMode == CompletionMode.ONE_STATEMENT) {
                settings.completionMode = CompletionMode.MULTILINE;
                LOG.debug("Chatx completion mode was changed from one statement to multiline");
            }
        }
        agentData.setCompletionStatus(CompletionStatus.ACCEPT);
        String uuid = agentData.getUuid();
        ChatUtil.updateCodeCompletionLogStatus(uuid, agentData.getCompletionStatus());
    }

    @Override
    public void sendRejectedTelemetry(@NotNull ChatxCompletion chatxCompletion) {
        GetCompletionsResult.Completion agentData = ((AgentCompletion) chatxCompletion).getAgentData();
        if (agentData.getCompletionStatus() != CompletionStatus.SHOWN) {
            LOG.debug("Chatx completion was not in shown status, not sending accepted telemetry, current status"
                    + agentData.getCompletionStatus());
            return;
        }
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        settings.consecutiveApplyCount = 0;
        if ("server".equals(settings.lineMode)) {
            if (settings.completionMode == CompletionMode.ONE_STATEMENT) {
                settings.completionMode = CompletionMode.ONE_LINE;
                LOG.debug("Chatx completion mode was changed from one statement to one line");
            } else if (settings.completionMode == CompletionMode.MULTILINE) {
                settings.completionMode = CompletionMode.ONE_STATEMENT;
                LOG.debug("Chatx completion mode was changed from multiline to one statement");
            } else if (settings.completionMode == CompletionMode.ONE_LINE) {
                settings.consecutiveRejectCount++;
            }
        }
        // TODO send telemetry
    }

    @Override
    public void dispose() {
        reset();
    }

    public void reset() {
        this.cache.clear();
    }


    public static class AgentCompletionList implements ChatxInlayList {
        private final ChatxInlayList inlays;

        private final AgentCompletion completion;

        @NotNull
        private final EditorRequest request;

        private String replacementText;

        public AgentCompletionList(@Nullable ChatxInlayList inlays, @NotNull AgentCompletion completion
                , @NotNull EditorRequest request) {
            this.inlays = inlays;
            this.completion = completion;
            this.request = request;
            //TODO: lzh 验证是否bug
            this.replacementText = inlays.getReplacementText();
//            this.replacementText = CompletionUtil.dropOverlappingTrailingLines(completion.getAgentData().getText()
//                    , request.getDocumentContent(), request.getOffset());
        }

        @Override
        public boolean isEmpty() {
            return (this.inlays == null || this.inlays.isEmpty());
        }

        @Override
        public @NotNull ChatxCompletion getChatxCompletion() {
            return completion;
        }

        @Override
        public @NotNull TextRange getReplacementRange() {
            return inlays.getReplacementRange();
        }

        @Override
        public @NotNull String getReplacementText() {
            return this.replacementText;
        }

        @Override
        public @NotNull List<ChatxEditorInlay> getInlays() {
            return (inlays == null) ? Collections.emptyList() : inlays.getInlays();
        }

        @Override
        public int getOffset() {
            return request.getOffset();
        }

        @Override
        public void setReplacementText(String text) {
            this.replacementText = replacementText;
        }

        @Override
        public void setReplacementRange(TextRange range) {
            inlays.setReplacementRange(range);
        }

        @NotNull
        @Override
        public Iterator<ChatxEditorInlay> iterator() {
            return (inlays != null) ? inlays.iterator() : Collections.emptyIterator();
        }
    }
}
