package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局配置实体类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig implements AbstractEditorItem<GlobalConfig> {
    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;

    @Override
    public GlobalConfig defaultVal() {
        return new GlobalConfig("demo", "value");
    }

    @Override
    public void changeFileName(String name) {
        this.name = name;
    }

    @Override
    public String fileName() {
        return this.name;
    }

    @Override
    public void changeFileContent(String content) {
        this.value = content;
    }

    @Override
    public String fileContent() {
        return this.value;
    }
}
