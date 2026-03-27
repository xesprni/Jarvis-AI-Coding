package com.qihoo.finance.lowcode.codereview.entity.dto;

import lombok.Data;

import java.util.Date;

/**
 * CodeRvCommit
 *
 * @author fengjinfu-jk
 * date 2023/11/7
 * @version 1.0.0
 * @apiNote CodeRvCommit
 */
@Data
public class CodeRvComment {
    /**
     * 评论人
     */
    private String author;
    /**
     * 评论内容
     */
    private String body;
    /**
     * 评论创建时间
     */
    private Date createdAt;
    /**
     * 评论更新时间
     */
    private Date updatedAt;
    /**
     * 是否解决
     */
    private boolean resolved;
    /**
     * 解决人
     */
    private String resolvedBy;
    /**
     * 解决时间
     */
    private String resolvedAt;
    /**
     * 代码位置信息
     */
    private Position position = new Position();

    @Data
    public static class Position {
        /**
         * 代码旧路径
         * chatx-expert/src/main/java/com/qihoo/finance/lowcode/apitrack/dialog/table/JsonFormTableWrap.java
         */
        private String oldPath;
        /**
         * 代码新路径
         * chatx-expert/src/main/java/com/qihoo/finance/lowcode/apitrack/dialog/table/JsonFormTableWrap.java
         */
        private String newPath;
        /**
         * 类型 text
         */
        private String positionType;
        private int oldLine;
        private int newLine;
        private LineRange lineRange;

        @Data
        public static class LineRange {
            private LineRangeItem start;
            private LineRangeItem end;

            @Data
            public static class LineRangeItem {
                private int newLine;
                private int oldLine;
            }
        }
    }
}
