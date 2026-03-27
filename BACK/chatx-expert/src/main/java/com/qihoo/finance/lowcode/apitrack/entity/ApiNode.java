package com.qihoo.finance.lowcode.apitrack.entity;

import com.qihoo.finance.lowcode.common.ui.base.FilterableTreeNode;
import lombok.Getter;
import lombok.Setter;

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
public class ApiNode extends FilterableTreeNode {
    private ApiGroupNode apiGroupNode;
    private String projectToken;
    private String name;
    private String code;
    private String url;

    // 暂时没有 status, private getter
    private String status;
    // 暂时没有 tag, private getter
    private String tag;

    private RequestMethod method;

    // code
    private Long id;
    // name
    private String title;
    // url
    private Long categoryId;
    private String methodName;

    private boolean editable;

    // 暂时没有 status, private getter
    private String getStatus() {
        return status;
    }

    // 暂时没有 tag, private getter
    private String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", method, name, url);
    }
}
