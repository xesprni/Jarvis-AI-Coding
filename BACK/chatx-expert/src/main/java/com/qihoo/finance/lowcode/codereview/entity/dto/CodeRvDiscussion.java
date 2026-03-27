package com.qihoo.finance.lowcode.codereview.entity.dto;

import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import lombok.Data;

import java.util.List;

/**
 * CodeRvDiscussion
 *
 * @author fengjinfu-jk
 * date 2023/11/7
 * @version 1.0.0
 * @apiNote CodeRvCommit
 */
@Data
public class CodeRvDiscussion {
    private String id;
    private String overview;
    private boolean resolved;
    private boolean individualNote;
    private CodeRvRepoNode repoNode = new CodeRvRepoNode();
    private CodeRvTaskNode taskNode = new CodeRvTaskNode();
    private List<CodeRvComment> comments;
}
