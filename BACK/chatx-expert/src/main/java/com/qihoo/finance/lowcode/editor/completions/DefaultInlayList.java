package com.qihoo.finance.lowcode.editor.completions;

import com.intellij.openapi.util.TextRange;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

@Data
@RequiredArgsConstructor
public class DefaultInlayList implements ChatxInlayList {

    @NotNull
    final ChatxCompletion chatxCompletion;
    @NotNull
    TextRange replacementRange;
    /** 计算重叠缩进后要替换的文本 */
    @NotNull
    String replacementText;
    final List<ChatxEditorInlay> inlays;

    @Override
    public boolean isEmpty() {
        return this.inlays.isEmpty();
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @NotNull
    @Override
    public Iterator<ChatxEditorInlay> iterator() {
        return this.inlays.iterator();
    }
}
