package com.qihoo.finance.lowcode.editor;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletionService;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletionType;
import com.qihoo.finance.lowcode.editor.completions.ChatxEditorInlay;
import com.qihoo.finance.lowcode.editor.completions.ChatxInlayList;
import com.qihoo.finance.lowcode.editor.completions.CompletionSplitter;
import com.qihoo.finance.lowcode.editor.completions.CompletionUtil;
import com.qihoo.finance.lowcode.editor.lang.agent.AgentCompletion;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.request.RequestId;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ChatxApplicationState;
import com.qihoo.finance.lowcode.status.ChatxStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class ChatxEditorManagerImpl implements ChatxEditorManager {

    private static final Logger LOG = Logger.getInstance(ChatxEditorManagerImpl.class);

    private static final Key<Boolean> KEY_EDITOR_SUPPORTED = Key.create("chatx.editorSupported");
    public static final Key<EditorRequestResultList> KEY_LAST_REQUEST = Key.create("chatx.editorRequest");
    private static final Key<Boolean> KEY_PROCESSING = KeyWithDefaultValue.create("chatx.processing", Boolean.FALSE);
    static final Key<Boolean> KEY_DOCUMENT_SAVE_VETO = Key.create("chatx.docSaveVeto");
    private static final Set<String> COMMAND_BLACKLIST = Set.of("Expand Live Template by Tab");
    static InlayDisposeContext inlayDisposeContext = null;
    static String acceptNextWordText = "";
    private final CancellableAlarm requestAlarm = new CancellableAlarm(this);

    @Override
    @RequiresEdt
    public boolean isAvailable(@NotNull Editor editor) {
        Boolean isAvailable = KEY_EDITOR_SUPPORTED.get(editor);
        if (isAvailable == null) {
            isAvailable = !(editor instanceof com.intellij.injected.editor.EditorWindow)
                    && !(editor instanceof com.intellij.openapi.editor.impl.ImaginaryEditor)
                    && (!(editor instanceof EditorEx) || !((EditorEx) editor).isEmbeddedIntoDialogWrapper())
                    && !editor.isViewer() && !editor.isOneLineMode()
                    && ChatxCompletionService.getInstance().isAvailable(editor);
            KEY_EDITOR_SUPPORTED.set(editor, isAvailable);
        }
        return isAvailable && !editor.isDisposed();
    }

    @Override
    public int countCompletionInlays(@NotNull Editor editor, @NotNull TextRange searchRange
            , boolean inlineInlays, boolean afterLineEndInlays, boolean blockInlays, boolean matchInLeadingWhitespace) {
        if (!isAvailable(editor)) {
            return 0;
        }
        int startOffset = searchRange.getStartOffset();
        int endOffset = searchRange.getEndOffset();
        InlayModel inlayModel = editor.getInlayModel();
        int totalCount = 0;
        if (inlineInlays) {
            totalCount += (int)inlayModel.getInlineElementsInRange(startOffset, endOffset).stream().filter(inlay -> {
               if (!(inlay.getRenderer() instanceof ChatxInlayRenderer)) {
                   return false;
               }
               if (!matchInLeadingWhitespace) {
                   // 判断这个inlay是否都是空白字符
                   List<String> lines = ((ChatxInlayRenderer) inlay.getRenderer()).getContentLines();
                   if (lines.isEmpty()) {
                       return false;
                   }
                   int whiteSpaceEnd = inlay.getOffset() + ChatxStringUtil.leadingWhitespaceLength(lines.get(0));
                   return endOffset >= whiteSpaceEnd;
               }
               return true;
            }).count();
        }
        if (blockInlays) {
            totalCount = (int)(totalCount + inlayModel.getBlockElementsInRange(startOffset, endOffset).stream()
                    .filter(inlay -> inlay.getRenderer() instanceof ChatxInlayRenderer).count());
        }
        if (afterLineEndInlays)
            totalCount = (int)(totalCount + inlayModel.getAfterLineEndElementsInRange(startOffset, endOffset).stream()
                    .filter(inlay -> inlay.getRenderer() instanceof ChatxInlayRenderer).count());
        return totalCount;
    }

    @Override
    public boolean hasTypingAsSuggestedData(Editor editor, char next) {
        EditorRequestResultList request = KEY_LAST_REQUEST.get(editor);
        if (request == null) {
            return false;
        }
        List<ChatxInlayList> cached = ChatxCompletionService.getInstance().fetchCachedCompletions(request.getRequest());
        if (cached == null || cached.isEmpty()) {
            return false;
        }
        ChatxInlayList first = cached.get(0);
        if (first.isEmpty()) {
            return false;
        }
        ChatxEditorInlay firstInlay = first.iterator().next();
        if (firstInlay.getLines().isEmpty()) {
            return false;
        }
        if (!firstInlay.getLines().get(0).startsWith(String.valueOf(next))) {
            return false;
        }
        if (first.getChatxCompletion() instanceof AgentCompletion) {
            AgentCompletion agentCompletion = (AgentCompletion) first.getChatxCompletion();
            String fullInlay = agentCompletion.getAgentData().getText().stripLeading();
            // 去除当前正在敲入的字符
            String displayedInlay = firstInlay.getLines().get(0).substring(1);
            return fullInlay.indexOf(displayedInlay) < 3;
        }
        return true;
    }

    @Override
    public void disposeInlays(@NotNull Editor editor, @NotNull InlayDisposeContext disposeContext) {
        if (!isAvailable(editor) || isProcessing(editor)) {
            return;
        }
        EditorRequestResultList request = KEY_LAST_REQUEST.get(editor);
        if (disposeContext.isResetLastRequest()) {
            KEY_LAST_REQUEST.set(editor, null);
        }
        if (request == null || request.getRequest().getOffset() != editor.getCaretModel().getOffset()) {
            cancelCompletionRequests(editor);
        }
        wrapProcessing(editor, () -> disposeInlays(collectInlays(editor, 0, editor.getDocument().getTextLength())));
    }

    private void disposeInlays(List<ChatxInlayRenderer> renderers) {
        if (!renderers.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Disposing inlays, size: " + renderers.size());
            }
            for (ChatxInlayRenderer renderer : renderers) {
                Inlay<ChatxInlayRenderer> inlay = renderer.getInlay();
                if (inlay != null)
                    Disposer.dispose(inlay);
            }
        }
    }

    @NotNull
    @RequiresEdt
    public List<ChatxInlayRenderer> collectInlays(@NotNull Editor editor, int startOffset, int endOffset) {
        InlayModel model = editor.getInlayModel();
        ArrayList<Inlay<?>> inlays = new ArrayList<>();
        inlays.addAll(model.getInlineElementsInRange(startOffset, endOffset));
        inlays.addAll(model.getAfterLineEndElementsInRange(startOffset, endOffset));
        inlays.addAll(model.getBlockElementsInRange(startOffset, endOffset));
        ArrayList<ChatxInlayRenderer> renderers = new ArrayList<>();
        for (Inlay<?> inlay : inlays) {
            if (inlay.getRenderer() instanceof ChatxInlayRenderer)
                renderers.add((ChatxInlayRenderer)inlay.getRenderer());
        }
        return renderers;
    }

    private void wrapProcessing(@NotNull Editor editor, @NotNull Runnable block) {
        assert !KEY_PROCESSING.get(editor);
        try {
            KEY_PROCESSING.set(editor, Boolean.TRUE);
            block.run();
        } finally {
            KEY_PROCESSING.set(editor, null);
        }
    }

    private boolean isProcessing(@NotNull Editor editor) {
        return KEY_PROCESSING.get(editor);
    }

    @Override
    @RequiresEdt
    public void editorModified(@NotNull Editor editor, int offset, @NotNull CompletionRequestType requestType) {
        LOG.debug("editorModified");
        if (ChatxStatusService.isClientRequestsDisabled()) {
            LOG.debug("Completions disabled because of the status.");
            return;
        }
        if (ChatxApplicationSettings.settings().fetchCompletionAfter > System.currentTimeMillis()) {
            LOG.debug("Fetching completion is disabled before: ", ChatxApplicationSettings.settings().fetchCompletionAfter);
            return;
        }
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        EditorRequestResultList lastRequest = KEY_LAST_REQUEST.get(editor);
        if (isDuplicateRequest(editor.getDocument(), offset, requestType, lastRequest)) {
            LOG.debug("Ignoring duplicate editorModified request");
            return;
        }
        if (isLookupUnsupported(requestType, editor)) {
            LOG.debug("completion disabled because of a visible popup");
//            disposeInlays(editor, InlayDisposeContext.CaretChange);
            return;
        }
        cancelCompletionRequests(editor);
        if (isBlacklistedCommand()) {
            return;
        }
        if (isUnsupportedEditorState(editor)) {
            return;
        }
        if (requestType.isAutomaticOrForced() && !ChatxApplicationSettings.isChatxEnabled(project, editor)) {
            disposeInlays(editor, InlayDisposeContext.Typing);
            ApplicationManager.getApplication().getMessageBus().syncPublisher(CompletionsRejectedMessage.TOPIC)
                    .automaticCompletionsRejected(null);
            return;
        }
        if (editor.getDocument().getText().length() < 8) {
            return;
        }
        if (middleOfLineWontComplete(editor, offset)) {
            disposeInlays(editor, InlayDisposeContext.Typing);
            return;
        }
        if (ChatxApplicationSettings.settings().isTriggerByKey && requestType != CompletionRequestType.Manual) {
            return;
        }
        CompletionType completionType = CompletionType.GHOST_TEXT;
        if (CompletionRequestType.GENERATE_CODE_BY_COMMENT == requestType) {
            completionType = CompletionType.GENERATE_CODE_BY_COMMENT;
        }
        EditorRequest request = ChatxCompletionService.getInstance().createRequest(editor, offset
                , completionType);
        if (request == null) {
            return;
        }
        ChatxEditorUtil.addEditorRequest(editor, request);
        KEY_LAST_REQUEST.set(editor, new EditorRequestResultList(request));
        if (lastRequest != null && requestType.isUnforced()) {
            List<ChatxInlayList> typingAsSuggested = ChatxCompletionService.getInstance().fetchCachedCompletions(request);
            if (typingAsSuggested != null && !typingAsSuggested.isEmpty()) {
                Collections.reverse(typingAsSuggested);
                insertInlays(typingAsSuggested.get(0), request, editor, true, InlayDisposeContext.TypingAsSuggested);
                if (!addInlays(editor, typingAsSuggested)) {
                    LOG.debug("failed to add inlays for typing-as-suggested");
                }
                return;
            }
        }
        disposeInlays(editor, InlayDisposeContext.Typing);
        if (lastRequest != null && lastRequest.getCurrentCompletion() != null) {
            ChatxCompletionService.getInstance().sendRejectedTelemetry(lastRequest.getCurrentCompletion().getChatxCompletion());
        }
        ChatxApplicationState settings = ChatxApplicationSettings.settings();
        if ("server".equals(settings.lineMode) && settings.consecutiveRejectCount > 0
                && settings.completionMode == CompletionMode.ONE_LINE) {
            int freezeCompletionInterval = settings.freezeCompletionIntervals.get(settings.freezeCompletionIntervals.size() - 1);
            if (settings.consecutiveRejectCount < settings.freezeCompletionIntervals.size() - 1) {
                freezeCompletionInterval = settings.freezeCompletionIntervals.get(settings.consecutiveRejectCount - 1);
            }
            long nextCompletionTIme = settings.lastFetchCompletionTime + freezeCompletionInterval * 1000L;
            if (System.currentTimeMillis() < nextCompletionTIme) {
                LOG.debug("skip completion due to consecutive reject freeze, next completion time: " + nextCompletionTIme);
                return;
            }
        }
        if (ChatxEditorSupport.isEditorCompletionsSupported(editor)) {
            settings.lastFetchCompletionTime = System.currentTimeMillis();
            queueCompletionRequest(editor, request, requestType, null, false, first ->
                    insertInlays(first, request, editor, false, InlayDisposeContext.Typing));
        }
    }

    @RequiresEdt
    public boolean applyCompletion(@NotNull Editor editor, ChatxApplyInlayStrategy applyStrategy) {
        if (editor.isDisposed()) {
            LOG.warn("editor already disposed");
            return false;
        }
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            LOG.warn("project disposed or null: " + project);
            return false;
        }
        if (isProcessing(editor)) {
            LOG.warn("can't apply1 inlays while processing");
            return false;
        }
        EditorRequestResultList request = KEY_LAST_REQUEST.get(editor);
        if (request == null) {
            return false;
        }
        ChatxInlayList currentCompletion = request.getCurrentCompletion();
        if (currentCompletion == null) {
            return false;
        }
        if (applyStrategy == ChatxApplyInlayStrategy.WHOLE) {
            disposeInlays(editor, InlayDisposeContext.Applied);
        } else {
            disposeInlays(editor, InlayDisposeContext.TypingAsSuggested);
        }
        applyCompletion(project, editor, request.getRequest(), currentCompletion, applyStrategy);
        ChatxCompletionService.getInstance().sendAcceptedTelemetry(currentCompletion.getChatxCompletion());
        return true;
    }

    @RequiresEdt
    public void applyCompletion(@NotNull Project project, @NotNull Editor editor, @NotNull EditorRequest request
            , @NotNull ChatxInlayList completion, @NotNull ChatxApplyInlayStrategy applyStrategy) {
        TextRange range = completion.getReplacementRange();
        WriteCommandAction.runWriteCommandAction(project, "Apply ChatX Suggestion", "ChatX", () -> {
            if (project.isDisposed()) {
                return;
            }
            Document document = editor.getDocument();
            try {
                KEY_DOCUMENT_SAVE_VETO.set(document, Boolean.TRUE);
            } finally {
                KEY_DOCUMENT_SAVE_VETO.set(document, null);
            }
            // 空行才可以删空白字符（非空行做了diff，删除空白字符会导致空格错乱），空白字符包含光标前的空白字符和光标后的空白字符
            if (range.getLength() > 0 && StringUtils.isBlank(request.getLineInfo().getLine())) {
                document.deleteString(range.getStartOffset(), range.getEndOffset());
            }
            int insertedLength = 0;
            int caretPos = 0;
            int i = 0;
            boolean fullInsert = false;
            for (; i < completion.getInlays().size(); i++) {
                ChatxEditorInlay inlay = completion.getInlays().get(i);
                String text = StringUtils.join(inlay.getLines(), "\n");
                if (inlay.getType() == ChatxCompletionType.Block) {
                    text = "\n" + text;
                }
                if (applyStrategy != ChatxApplyInlayStrategy.WHOLE) {
                    String textToInsert = CompletionSplitter.split(text, applyStrategy);
                    document.insertString(inlay.getEditorOffset(), textToInsert);
                    caretPos = inlay.getEditorOffset() + textToInsert.length();
                    if (textToInsert.length() == text.length() && i == completion.getInlays().size()) {
                        fullInsert = true;
                    }
                    break;
                }
                int offset = inlay.getEditorOffset() + insertedLength;
                document.insertString(offset, text);
                caretPos = inlay.getEditorOffset() + insertedLength + text.length();
                insertedLength += text.length();
                fullInsert = true;
            }
            editor.getCaretModel().moveToOffset(caretPos);
            if (ChatxApplicationSettings.settings().enableSyntaxCorrection) {
                LOG.debug("syntax correction start");
                CompletionUtil.correctSyntax(editor);
            }
            if (i < completion.getInlays().size() || !fullInsert) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    editorModified(editor, editor.getCaretModel().getOffset(), CompletionRequestType.Automatic);
                });
            }
        });
    }

    @RequiresBackgroundThread
    private void requestChatxCompletions(@NotNull final Editor editor, @NotNull final EditorRequest request
            , @Nullable Integer maxCompletions, boolean cycling, @Nullable final Consumer<ChatxInlayList> onFirstCompletion) {
        final AtomicBoolean resetCompletions = new AtomicBoolean(cycling);
        ChatxCompletionService.getInstance().fetchCompletions(request, maxCompletions, cycling, new Flow.Subscriber<>() {
                    private volatile Flow.Subscription subscription;
                    private volatile boolean hasFirstCompletion;

                    public void onSubscribe(Flow.Subscription subscription) {
                        (this.subscription = subscription).request(1L);
                        Objects.requireNonNull(this.subscription);
                        Objects.requireNonNull(this.subscription);
                        Disposer.tryRegister(request.getDisposable(), this.subscription::cancel);
                    }

                    public void onNext(List<ChatxInlayList> inlaySets) {
                        LOG.debug("received inlay");
                        if (!isActiveRequest(request, editor)) {
                            LOG.debug("skipping inlay because request already cancelled");
                            return;
                        }
                        if (resetCompletions.compareAndSet(true, false)) {
                            EditorRequestResultList stored = KEY_LAST_REQUEST.get(editor);
                            if (stored != null) {
                                stored.resetInlays();
                            }
                        }
                        if (!addInlays(editor, inlaySets)) {
                            LOG.debug("failed to add inlays");
                            return;
                        }
                        this.subscription.request(1L);
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(InlaysReceivedMessage.TOPIC)
                                .inlaysReceived(request, inlaySets);
                        if (!this.hasFirstCompletion && onFirstCompletion != null && !inlaySets.isEmpty()) {
                            this.hasFirstCompletion = true;
                            ChatxInlayList firstSet = inlaySets.get(0);
                            assert firstSet != null && !firstSet.isEmpty();
                            ApplicationManager.getApplication().invokeLater(() -> onFirstCompletion.accept(firstSet));
                        }
                    }

                    public void onError(Throwable throwable) {
                        if (LOG.isTraceEnabled() || ApplicationManager.getApplication().isUnitTestMode()) {
                            LOG.debug("onError", throwable);
                        } else if (LOG.isDebugEnabled()) {
                            LOG.debug("onError: " + throwable.getMessage());
                        }
                        //TODO 判断是否未登录
                    }

                    public void onComplete() {
                    }
                }, editor);
    }

    private void insertInlays(@NotNull ChatxInlayList inlays, @NotNull EditorRequest request, @NotNull Editor editor
            , boolean disposeExistingInlays, @NotNull InlayDisposeContext disposeContext) {
        if (!isActiveRequest(request, editor)) {
            LOG.debug("skipping insertion of inlay because request was cancelled");
            return;
        }
        // 光标后的提示内容与光标大于等于一个tab时，不显示提示，避免误应用补全
        if (StringUtils.isBlank(request.getLineInfo().getLine())) {
            String firstLine = inlays.getInlays().get(0).getLines().get(0);
            int completionSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(firstLine, request.getTabWidth());
            if (inlays.getInlays().get(0).getType() == ChatxCompletionType.Block) {
                int linePrefixSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(request.getLineInfo().getLinePrefix(), request.getTabWidth());
                if (completionSpaceWidth - linePrefixSpaceWidth >= request.getTabWidth()) {
                    return;
                }
            } else {
                if (completionSpaceWidth >= request.getTabWidth()) {
                    return;
                }
            }
        }
        if (request.getOffset() != editor.getCaretModel().getOffset()) {
            LOG.info("skipping insertion of inlay because request offset is not equal to caret offset, request offset: "
                    + request.getOffset() + ", caret offset: " + editor.getCaretModel().getOffset());
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("inserting completion inlay into editor, current request ID: " + RequestId.currentRequestId() +
                    ", request ID: " + request.getRequestId() + ", caret: " + editor.getCaretModel().getOffset() +
                    ", request offset: " + request.getOffset());
        }
        if (!editor.isDisposed()) {
            doInsertInlays(inlays, request, editor, disposeExistingInlays, disposeContext);
        }
    }

    private void doInsertInlays(@NotNull ChatxInlayList inlays, @NotNull EditorRequest request, @NotNull Editor editor
            , boolean disposeExistingInlays, @NotNull InlayDisposeContext context) {
        if (disposeExistingInlays) {
            disposeInlays(editor, context);
        }
        ArrayList<Inlay<ChatxInlayRenderer>> insertedInlays = new ArrayList<>();
        InlayModel inlayModel = editor.getInlayModel();
        int index = 0;
        for (ChatxEditorInlay inlay : inlays) {
            ChatxDefaultInlayRenderer renderer;
            if (inlay.isEmptyCompletion()) {
                continue;
            }
            int caretOffset = editor.getCaretModel().getOffset();
            int startOffset = inlays.getReplacementRange().getStartOffset();
            String documentContent = editor.getDocument().getText();
            if (startOffset > documentContent.length()) {
                return;
            }
            String caretPrefix = documentContent.substring(0, caretOffset);
            String startPrefix = documentContent.substring(0, startOffset);
            String caretTrimmed = CompletionUtil.TrimEndSpaceTab(caretPrefix);
            String startTrimmed = CompletionUtil.TrimEndSpaceTab(startPrefix);
            if (!caretTrimmed.equals(startTrimmed)) {
                LOG.info("Insert inlays skipped due to content not equals, Caret trimmed: " + caretTrimmed + " start trimmed: " + startTrimmed);
                return;
            }
            int moveCaret = 0;
            if (inlays.getReplacementRange().getLength() > 0 && context != InlayDisposeContext.TypingAsSuggested) {
                moveCaret = inlays.getReplacementRange().getStartOffset() - caretOffset;
            } else {
                inlayDisposeContext = InlayDisposeContext.TypingAsSuggested;
            }
            renderer = new ChatxDefaultInlayRenderer(editor, request, inlay.getType(), inlay.getLines());
            Inlay<ChatxInlayRenderer> editorInlay = null;
            switch (inlay.getType()) {
                case Inline:
                    if (moveCaret < 0) {
                        editor.getCaretModel().moveToOffset(caretOffset + moveCaret);
                    }
                    editorInlay = inlayModel.addInlineElement(inlay.getEditorOffset(), true
                            , Integer.MAX_VALUE - index, renderer);
                    break;
                case AfterLineEnd:
                    editorInlay = inlayModel.addAfterLineEndElement(inlay.getEditorOffset(), true, renderer);
                    break;
                case Block:
                    editorInlay = inlayModel.addBlockElement(inlay.getEditorOffset(), true
                            , false, Integer.MAX_VALUE - index, renderer);
                    break;
            }
            if (editorInlay != null) {
                renderer.setInlay(editorInlay);
            }
            insertedInlays.add(editorInlay);
            index++;
        }
        ChatxCompletionService.getInstance().sendShownTelemetry(request, inlays.getChatxCompletion(), editor);
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(ChatxInlayListener.TOPIC)
                .inlaysUpdated(request, context, editor, insertedInlays);
    }

    private boolean isActiveRequest(@NotNull EditorRequest request, @NotNull Editor editor) {
        if (request.getRequestId() != RequestId.currentRequestId()) {
            return false;
        }
        EditorRequestResultList stored = KEY_LAST_REQUEST.get(editor);
        return stored != null && stored.getRequest().equalsRequest(request);
    }

    private boolean addInlays(@NotNull Editor editor, @NotNull List<ChatxInlayList> inlaySets) {
        EditorRequestResultList stored = KEY_LAST_REQUEST.get(editor);
        if (stored != null) {
            for (ChatxInlayList inlays : inlaySets) {
                stored.addInlays(inlays);
            }
        }
        return (stored != null);
    }

    /**
     * 取消补全HTTP请求
     * @param editor
     */
    public void cancelCompletionRequests(@NotNull Editor editor) {
        this.requestAlarm.cancelAllRequests();
        List<EditorRequest> requests = ChatxEditorUtil.KEY_REQUESTS.get(editor);
        if (requests == null || requests.isEmpty())
            return;
        int count = requests.size();
        Iterator<EditorRequest> requestIterator = requests.iterator();
        while (requestIterator.hasNext()) {
            EditorRequest request = requestIterator.next();
            requestIterator.remove();
            request.cancel();
        }
        ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorRequestsCancelledMessage.TOPIC)
                .requestsCancelled(count);
    }

    private boolean isDuplicateRequest(@NotNull Document document, int requestOffset
            , @NotNull CompletionRequestType requestType, @Nullable EditorRequestResultList lastRequest) {
        if (lastRequest == null || requestType.isForcedOrManual())
            return false;
        if (lastRequest.getRequest().getOffset() != requestOffset)
            return false;
        long lastStamp = lastRequest.getRequest().getDocumentModificationSequence();
        return (lastStamp == ChatxEditorUtil.getDocumentModificationStamp(document));
    }

    private boolean isLookupUnsupported(@NotNull CompletionRequestType requestType, @NotNull Editor editor) {
        return (requestType.isAutomaticOrForced() &&
                !ChatxApplicationSettings.settings().isShowIdeCompletions() &&
                LookupManager.getActiveLookup(editor) != null);
    }

    private boolean isBlacklistedCommand() {
        String commandName = CommandProcessor.getInstance().getCurrentCommandName();
        return (commandName != null && COMMAND_BLACKLIST.contains(commandName));
    }

    private boolean isUnsupportedEditorState(@NotNull Editor editor) {
        if (editor.getCaretModel().getCaretCount() > 1)
            return true;
        return editor.getSelectionModel().hasSelection();
    }

    private boolean middleOfLineWontComplete(@NotNull Editor editor, int offset) {
        Project project = editor.getProject();
        if (project == null) {
            return false;
        }
        if (!ChatxApplicationSettings.settings().enableMiddleCompletion) {
            return false;
        }
        Document document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return false;
        }
        String trailingBlock = document.getText().substring(offset);
        if (trailingBlock.isBlank()) {
            return false;
        }
        String[] res = trailingBlock.lines().toArray(String[]::new);
        String trailingText = res[0];
        // 如果结尾不都是这些字符(包含用户敲的代码)则不补全
        return !trailingText.matches("[]{}):; \n\r\t'\"]*");
    }

    private boolean isAtTheMiddleOfLine(@NotNull Editor editor, int offset) {
        try {
            Project project = editor.getProject();
            if (project == null) {
                return false;
            }
            Document document = editor.getDocument();
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (file == null) {
                return false;
            }
            String trailingBlock = document.getText().substring(offset);
            if (trailingBlock.isBlank()) {
                return false;
            }
            String[] res = trailingBlock.lines().toArray(String[]::new);
            String trailingText = res[0];
            return !trailingText.isBlank();
        } catch (Exception e) {
            LOG.warn("error in isAtTheMiddleOfLine", e);
            return false;
        }
    }

    private void queueCompletionRequest(@NotNull Editor editor, @NotNull EditorRequest contentRequest
            , @NotNull CompletionRequestType requestType, @Nullable Integer maxCompletions, boolean cycling
            , @Nullable Consumer<ChatxInlayList> onFirstCompletion) {
        int debounceMillis = (int)ChatxApplicationSettings.settings().debounceMillis;
        int n = 0;
        Integer minCompletionDelay = ChatxApplicationSettings.settings().minCompletionDelay;
        Double contextualFilterScore = ChatxCompletionService.getInstance().getLogTelemetries().getContextualFilterScore();
        if (contextualFilterScore >= 0.0D) {
            double r = 0.3475D;
            int i = 7;
            n = (int)(minCompletionDelay + 250.0D / (1.0D + Math.pow(contextualFilterScore / (double) r, 7.0D)));
        }
        if (requestType == CompletionRequestType.Automatic && contextualFilterScore >= 0.0D
                && contextualFilterScore < 0.35D && Math.random() < 0.75D)
            return;
        this.requestAlarm.cancelAllAndAddRequest(() -> {
            if (!contentRequest.isCancelled()) {
                requestChatxCompletions(editor, contentRequest, maxCompletions, cycling, onFirstCompletion);
            }
        }, debounceMillis);
    }

    @Override
    public void dispose() {
    }
}
