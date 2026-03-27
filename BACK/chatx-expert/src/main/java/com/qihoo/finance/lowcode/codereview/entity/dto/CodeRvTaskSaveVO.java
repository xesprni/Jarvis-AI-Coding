package com.qihoo.finance.lowcode.codereview.entity.dto;

import lombok.Data;

/**
 * CodeRvCommit
 *
 * @author fengjinfu-jk
 * date 2023/11/7
 * @version 1.0.0
 * @apiNote CodeRvCommit
 */
@Data
public class CodeRvTaskSaveVO {
    /**
     * 任务评审链接
     * mock: <a href="https://gitlab.daikuan.qihoo.net/360jr-base/lowcode/-/merge_requests/6">...</a>
     */
    private String webUrl;
}
