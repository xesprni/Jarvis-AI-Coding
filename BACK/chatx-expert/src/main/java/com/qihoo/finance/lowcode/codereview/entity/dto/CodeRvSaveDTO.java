package com.qihoo.finance.lowcode.codereview.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

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
public class CodeRvSaveDTO implements Serializable {

    /**
     * 评审任务id
     */
    private long projectId;
    /**
     * 评审任务标题
     */
    private String title;
    /**
     * 生成模式
     * 1: 基于2个commit 差异  2: 基于commit 列表
     */
    private int diffMode;
    /**
     * diffDetail
     */
    private DiffDetail diffDetail;
    /**
     * 关联的迭代版本
     */
    private String sprint;
    /**
     * 关联的任务 id
     */
    private String issue;
    /**
     * 评审人列表
     * 单个, 考虑兼容使用List<String>
     */
    private List<String> reviewers;

    // ~ edit
    //------------------------------------------------------------------------------------------------------------------
    /**
     * 评审任务 id
     */
    private long reviewId;
    /**
     * MR 关联的源分支
     */
    private String mrSourceBranch;
    /**
     * MR 关联的目标分支
     */
    private String mrTargetBranch;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffDetail {
        private String sourceBranch;
        private CodeRvCommit sourceCommit;
        private String targetBranch;
        private CodeRvCommit targetCommit;
        private String branch;
        private List<CodeRvCommit> commits;
    }
}
