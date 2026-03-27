package com.qihoo.finance.lowcode.aiquestion.dto;

import com.qihoo.finance.lowcode.common.enums.GitIndexStatus;
import lombok.Data;

/**
 * GitIndex
 *
 * @author fengjinfu-jk
 * date 2024/8/5
 * @version 1.0.0
 * @apiNote GitIndex
 */
@Data
public class GitIndex {
    private String status;
    private String reason;

    public static GitIndex of(String status) {
        GitIndex gitIndex = new GitIndex();
        gitIndex.setStatus(status);
        return gitIndex;
    }
}
