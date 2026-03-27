package com.qihoo.finance.lowcode.editor.lang.agent.dto;

import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.entity.dto.aiquestion.PromptElementRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CompletionRequest {

    private String prompt;
    private String suffix;
    private String lang;
    private Boolean isFimEnabled;
    /** server, block, linebyline */
    private String lineMode;
    /** codegeex-lite, codegeex-pro */
    private String model;
    private Double topK;
    private Double topP;
    private Double temperature;
    private CompletionType completionType;
    private List<PromptElementRange> promptElementRanges;
}
