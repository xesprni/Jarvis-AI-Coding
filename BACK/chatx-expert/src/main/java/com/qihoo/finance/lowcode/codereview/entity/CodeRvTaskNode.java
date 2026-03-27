package com.qihoo.finance.lowcode.codereview.entity;

import com.qihoo.finance.lowcode.codereview.entity.dto.CodeRvCommit;
import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 命名空间信息节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */
@Getter
@Setter
public class CodeRvTaskNode extends FilterableTreeNode {
    /**
     * 评审任务id
     */
    private long reviewId;
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
    private DiffDetail diffDetail = new DiffDetail();
    /**
     * 关联的迭代版本
     */
    private String sprint;
    /**
     * 关联的任务 id
     */
    private String issue;
    /**
     * 评审任务状态
     * opened closed ...
     */
    private String state;
    /**
     * 任务创建时间
     */
    private Date createdAt;
    /**
     * 任务评审链接
     */
    private String webUrl;
    /**
     * 评审人列表
     * 单个, 考虑兼容使用List<String>
     */
    private List<String> reviewers = new ArrayList<>();
    /**
     * MR 关联的源分支
     */
    private String mrSourceBranch;
    /**
     * MR 关联的目标分支
     */
    private String mrTargetBranch;

    public boolean isBaseSameBranch() {
        return 2 == this.diffMode;
    }

    public boolean isOpen() {
        return "opened".equalsIgnoreCase(state);
    }

    @Override
    public String toString() {
//        return String.format("%s  (%s)", code, name);
        return String.format("%s (%s)  %s", title, reviewId, (isOpen() ? "  [进行中]" : "  [已完成]"));
    }

    @Data
    public static class DiffDetail {
        private String sourceBranch;
        private CodeRvCommit sourceCommit;
        private String targetBranch;
        private CodeRvCommit targetCommit;
        private String branch;
        private List<CodeRvCommit> commits = new ArrayList<>();
    }
}
