package com.qihoo.finance.lowcode.editor.lang.agent;

import com.qihoo.finance.lowcode.common.entity.dto.askai.CompletionMode;
import com.qihoo.finance.lowcode.editor.completions.ChatxCompletion;
import com.qihoo.finance.lowcode.editor.completions.CompletionUtil;
import com.qihoo.finance.lowcode.editor.lang.agent.commands.GetCompletionsResult;
import com.qihoo.finance.lowcode.editor.lang.agent.lsp.Position;
import com.qihoo.finance.lowcode.editor.lang.agent.lsp.Range;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class AgentCompletion implements ChatxCompletion {

    private final GetCompletionsResult.Completion agentData;
    private final List<String> completion;
    private final boolean isCached;

    public AgentCompletion(GetCompletionsResult.Completion agentData) {
        this.agentData = agentData;
        this.completion = List.of(ChatxStringUtil.splitLines(agentData.getDisplayText()));
        this.isCached = false;
    }

    @Override
    public @NotNull List<String> getCompletion() {
        return completion;
    }

    @Override
    public @NotNull ChatxCompletion asCached() {
        return withCached(true);
    }

    private AgentCompletion withCached(boolean isCached) {
        return (this.isCached == isCached) ? this : new AgentCompletion(this.agentData, this.completion, isCached);
    }

    @Override
    public boolean isCached() {
        return isCached;
    }

    @Override
    public @Nullable ChatxCompletion withoutPrefix(@NotNull String prefix) {
        return CompletionUtil.apiChoiceWithoutPrefix(this, prefix);
    }

    @Override
    public @NotNull AgentCompletion withCompletion(@NotNull List<String> completion) {
        return (this.completion == completion) ? this : new AgentCompletion(agentData, completion, this.isCached);
    }

    public static AgentCompletion fromString(String uuid, String text, CompletionMode completionMode, String model) {
        GetCompletionsResult.Completion agentData = new GetCompletionsResult.Completion(uuid, text
                , Range.of(Position.of(0, 0), Position.of(0, 0)), text
                , Position.of(0, 0), completionMode, model);
        return new AgentCompletion(agentData);
    }
}
