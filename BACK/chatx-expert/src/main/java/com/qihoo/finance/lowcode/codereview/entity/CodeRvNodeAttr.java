package com.qihoo.finance.lowcode.codereview.entity;

import lombok.Data;

/**
 * CodeRvNodeAttr
 *
 * @author fengjinfu-jk
 * date 2023/11/6
 * @version 1.0.0
 * @apiNote CodeRvNodeAttr
 */
@Data
public class CodeRvNodeAttr {
    private String repo;
    private long projectId;

    public String getProjectIdStr() {
        return String.valueOf(projectId);
    }
}
