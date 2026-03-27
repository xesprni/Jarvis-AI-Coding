package com.qihoo.finance.lowcode.editor.lang.agent.dto;

import com.intellij.openapi.util.text.StringUtil;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.request.LineInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CompletionResult {

    String model;
    String[] code;

    public static CompletionResult fakeCompletionResult(EditorRequest request) {
        LineInfo lineInfo = request.getLineInfo();
        String[] code = {"String str = \"Hello lzh\";\nSystem.out.println(str);"};
        if (lineInfo.getLine().isBlank()) {
            code[0] = StringUtil.repeat(" ", lineInfo.getNextLineIndent()) + code[0];
            String[] split = code[0].split("\n");
            for (int i = 1; i < split.length; i++) {
                split[i] = StringUtil.repeat(" ", lineInfo.getNextLineIndent()) + split[i];
            }
            code[0] = StringUtil.join(split, "\n");
        }
        CompletionResult result = new CompletionResult();
        result.setCode(code);
        return result;
    }
}
