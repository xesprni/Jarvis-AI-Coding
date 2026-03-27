package com.qihoo.finance.lowcode.codereview.entity.dto;

import com.qihoo.finance.lowcode.common.utils.LocalDateUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

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
public class CodeRvCommit {

    private String commit;
    private String author;
    /**
     * commit 创建时间
     */
    private Date createdAt;
    /**
     * 提交信息
     */
    private String message;

    @Override
    public String toString() {
        return String.format("%s  【%s %s】",
                StringUtils.defaultString(message),
                StringUtils.defaultString(author),
                Objects.nonNull(createdAt) ? LocalDateUtils.convertToPatternString(createdAt, LocalDateUtils.FORMAT_DATE_TIME) : StringUtils.EMPTY);
    }
}
