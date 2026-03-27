package com.qihoo.finance.lowcode.design.entity;

import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.PlaceholderNode;
import com.qihoo.finance.lowcode.common.ui.base.PlaceTextNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
public class DatabaseNode extends SchemaNode implements PlaceTextNode {
    private String depName;
    private String code;
    private String name;
    private String parentCode;
    private String dataSourceType = Constants.DataSource.MySQL;
    private Map<String, Object> nodeAttr;
    private static final String INSTANCE_NAME = "instanceName";

    public String getInstanceName() {
        return (String) ObjectUtils.defaultIfNull(nodeAttr, new HashMap<>()).get(INSTANCE_NAME);
    }

    public DatabaseNode() {
        add(new PlaceholderNode());
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getDescription() {
        if (MapUtils.isNotEmpty(nodeAttr) && nodeAttr.containsKey(INSTANCE_NAME) && Objects.nonNull(nodeAttr.get(INSTANCE_NAME))) {
            return String.format("%s  %s", depName, nodeAttr.get(INSTANCE_NAME));
        }
        return null;
    }

    public String getDatabaseWithInstance() {
        return String.format("%s [%s]", name, getInstanceName());
    }
}
