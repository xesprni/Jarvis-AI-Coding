package com.qihoo.finance.lowcode.design.entity;

import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;

/**
 * 表字段节点
 *
 * @author fengjinfu-jk
 * date 2023/8/3
 * @version 1.0.0
 * @apiNote DataBaseMutableTreeNode
 */
@Getter
@Setter
public class DatabaseColumnNode extends DefaultMutableTreeNode implements PlaceTextNode {

    /**
     * 字段顺序
     */
    private int fieldNo;
    /**
     * 名称
     */
    private String fieldName;
    /**
     * 全类型
     */
    private String fieldType;
    /**
     * 长度
     */
    private Integer fieldLength;
    /**
     * 小数位
     */
    private Integer fieldPrecision;
    /**
     * 是否非空
     */
    private boolean isNotNull;
    /**
     * 是否主键
     */
    private boolean isPK;
    /**
     * 自增
     */
    private boolean isAutoIncr;
    /**
     * 无符号
     */
    private boolean isUnsigned;
    /**
     * 默认值
     */
    private Object fieldDefaults;
    /**
     * 注释, 字段备注
     */
    private String fieldComment;

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 标记是否为自定义附加列
     */
    private Boolean custom;
    /**
     * 扩展数据(JSON字符串)
     */
    private String ext;

    @Override
    public String getDescription() {
        String lengthTxt = null;
        if (Objects.nonNull(fieldLength) && fieldLength > 0 && !fieldType.contains("(")) {
            if (fieldPrecision > 0) {
                lengthTxt = String.format("(%s, %s)", fieldLength, fieldPrecision);
            } else {
                lengthTxt = String.format("(%s)", fieldLength);
            }
        }

        // fieldLength非空, >0 时, 才做拼接
        String fieldTypeStr = (Objects.nonNull(fieldLength) && fieldLength > 0 && StringUtils.isNotEmpty(lengthTxt)) ? fieldType + lengthTxt : fieldType;
        return String.format("%s  %s", fieldTypeStr, fieldComment);
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
