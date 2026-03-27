package com.qihoo.finance.lowcode.codereview.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CodeRvCommit
 *
 * @author fengjinfu-jk
 * date 2023/11/7
 * @version 1.0.0
 * @apiNote CodeRvCommit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRvBranch {
    private String branch;

    @Override
    public String toString() {
        return branch;
    }
}
