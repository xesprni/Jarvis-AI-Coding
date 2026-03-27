package com.qihoo.finance.lowcode.gentracker.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板信息类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template implements AbstractEditorItem<Template> {
    /**
     * 前置模板编码
     */
    private String preResourceCode;
    /**
     * 是否内部预置模板（必选不可取消）
     */
    private boolean innerTemplate;
    /**
     * 是否允许用户修改
     */
    private boolean canModify;
    /**
     * 是否强制覆盖
     */
    private boolean forcedOverlay;

    /**
     * 模板名称
     */
    private String name;
    /**
     * 模板代码
     */
    private String code;

    private String version;

    @Override
    public Template defaultVal() {
        return new Template(null, false, false, false, "demo", "template", "0");
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
        this.code = content;
    }

    @Override
    public String fileContent() {
        return this.code;
    }
}
