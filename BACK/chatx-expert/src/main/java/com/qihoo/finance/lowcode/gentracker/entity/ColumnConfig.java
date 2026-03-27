package com.qihoo.finance.lowcode.gentracker.entity;

import com.qihoo.finance.lowcode.gentracker.enums.ColumnConfigType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列配置信息
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@NoArgsConstructor
public class ColumnConfig implements AbstractItem<ColumnConfig> {
    /**
     * 标题
     */
    private String title;
    /**
     * 类型
     */
    private ColumnConfigType type;
    /**
     * 可选值，逗号分割
     */
    private String selectValue;

    public ColumnConfig(String title, ColumnConfigType type) {
        this.title = title;
        this.type = type;
    }

    public ColumnConfig(String title, ColumnConfigType type, String selectValue) {
        this.title = title;
        this.type = type;
        this.selectValue = selectValue;
    }

    @Override
    public ColumnConfig defaultVal() {
        return new ColumnConfig("demo", ColumnConfigType.TEXT);
    }
}
