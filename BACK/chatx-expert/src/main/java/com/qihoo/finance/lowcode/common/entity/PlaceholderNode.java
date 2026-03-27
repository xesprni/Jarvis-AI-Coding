package com.qihoo.finance.lowcode.common.entity;

import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * 加载中
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote LoadingNode
 */
@Getter
@Setter
public class PlaceholderNode extends FilterableTreeNode {
    private static final String placeholderText = "加载中, 请稍后...";
    private String text;

    public PlaceholderNode(String text) {
        this.text = text;
    }

    public PlaceholderNode() {
    }

    @Override
    public String toString() {
        return StringUtils.defaultString(text, placeholderText);
    }

    public boolean loading() {
        return StringUtils.isEmpty(text) || placeholderText.equals(text);
    }
}
