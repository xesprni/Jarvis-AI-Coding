package com.qihoo.finance.lowcode.editor.request;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.PromptElementRange;
import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.editor.util.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EditorRequest extends Cancellable {

    LineInfo getLineInfo();

    boolean equalsRequest(@NotNull EditorRequest paramEditorRequest);

    @NotNull
    default String getCurrentDocumentPrefix() {
        return getDocumentContent().substring(0, getOffset());
    }

    @NotNull
    String getDocumentContent();

    @NotNull
    Language getFileLanguage();

    @NotNull
    Project getProject();

    @NotNull
    CompletionType getCompletionType();

    int getOffset();

    void setOffset(int paramInt);

    boolean isUseTabIndents();

    int getTabWidth();

    int getRequestId();

    Disposable getDisposable();

    long getRequestTimestamp();

    long getDocumentModificationSequence();

    String getLang();
    String getPrompt();
    String getSuffix();

    /** 补全在的位置：method */
    String getCompletionLocation();

    List<PromptElementRange> getPromptElementRanges();

    /**
     * 从上下文中组装prompt，具体调模型前才需要调用此方法
     */
    @RequiresEdt
    void initPromptFromContext();

    CompletionMode getCompletionMode();
}
