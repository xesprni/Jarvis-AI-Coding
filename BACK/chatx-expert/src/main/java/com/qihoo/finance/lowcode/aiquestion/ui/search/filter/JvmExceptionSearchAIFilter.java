package com.qihoo.finance.lowcode.aiquestion.ui.search.filter;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.JvmExceptionOccurrenceFilter;
import com.intellij.execution.impl.InlayProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.qihoo.finance.lowcode.aiquestion.ui.search.renderer.SearchAIPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JvmExceptionSearchAIFilter
 *
 * @author fengjinfu-jk
 * date 2024/8/6
 * @version 1.0.0
 * @apiNote JavaExceptionAskAIFilter
 */
@SuppressWarnings("all")
public class JvmExceptionSearchAIFilter implements JvmExceptionOccurrenceFilter {
    @Override
    public @Nullable Filter.ResultItem applyFilter(@NotNull String exceptionClassName,
                                                   @NotNull List<PsiClass> classes,
                                                   int exceptionStartOffset) {
        return new CreateExceptionBreakpointResult(exceptionStartOffset, exceptionStartOffset + exceptionClassName.length(), exceptionClassName, ((PsiClass) classes.get(0)).getProject());
    }

    private static class CreateExceptionBreakpointResult extends Filter.ResultItem implements InlayProvider {
        private final String myExceptionClassName;
        private final Project project;
        private final int startOffset;

        CreateExceptionBreakpointResult(int highlightStartOffset, int highlightEndOffset, String exceptionClassName, Project project) {
            super(highlightStartOffset, highlightEndOffset, (HyperlinkInfo) null);
            this.myExceptionClassName = exceptionClassName;
            this.project = project;
            this.startOffset = highlightStartOffset;
        }

        public EditorCustomElementRenderer createInlayRenderer(Editor editor) {
            return new SearchAIPresentation(editor, this.project, this.startOffset, this.myExceptionClassName);
        }
    }
}
