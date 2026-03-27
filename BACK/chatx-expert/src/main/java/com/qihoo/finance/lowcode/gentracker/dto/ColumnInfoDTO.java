package com.qihoo.finance.lowcode.gentracker.dto;

import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.gentracker.entity.MatchType;
import com.qihoo.finance.lowcode.gentracker.entity.TypeMapper;
import com.qihoo.finance.lowcode.gentracker.tool.CurrGroupUtils;
import com.qihoo.finance.lowcode.gentracker.tool.NameUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 列信息传输对象
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@NoArgsConstructor
public class ColumnInfoDTO {

    public ColumnInfoDTO(DatabaseColumnNode field) {
        this.name = NameUtils.getInstance().getJavaName(field.getFieldName());
        this.comment = field.getFieldComment();
        this.type = getJavaType(field.getFieldType());
        this.isPK = field.isPK();
        this.custom = false;
        this.ext = "{}";
    }

    private String getJavaType(String dbType) {
        for (TypeMapper typeMapper : CurrGroupUtils.getCurrTypeMapperGroup().getElementList()) {
            if (typeMapper.getMatchType() == MatchType.ORDINARY) {
                if (dbType.equalsIgnoreCase(typeMapper.getColumnType())) {
                    return typeMapper.getJavaType();
                }
            } else {
                // 不区分大小写的正则匹配模式
                if (Pattern.compile(typeMapper.getColumnType(), Pattern.CASE_INSENSITIVE).matcher(dbType).matches()) {
                    return typeMapper.getJavaType();
                }
            }
        }
        return "java.lang.Object";
    }

    public boolean getIdentity() {
        return isPK || "id".equals(name);
    }

    /**
     * 名称
     */
    private String name;
    /**
     * 注释
     */
    private String comment;
    /**
     * 全类型
     */
    private String type;
    /**
     * jdbc类型
     */
    private String jdbcType;
    /**
     * 是否主键
     */
    private boolean isPK;
    /**
     * 标记是否为自定义附加列
     */
    private Boolean custom;

    public Boolean getCustom() {
        return !Objects.isNull(custom) && custom;
    }

    /**
     * 扩展数据(JSON字符串)
     */
    private String ext;
}
