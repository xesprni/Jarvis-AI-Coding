package com.qihoo.finance.lowcode.apitrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * JsonFormNode
 *
 * @author fengjinfu-jk
 * date 2023/9/18
 * @version 1.0.0
 * @apiNote JsonFormNode
 */
@Getter
@Setter
public class JsonFormNode {
    @JsonIgnore
    private JsonFormNode parent;
    private String name;
    private String type;
    private Map<String, JsonFormNode> properties = new LinkedHashMap<>();
    private String title;
    private String mock;
    private String description;
    private List<String> required = new ArrayList<>();
    private boolean currentRequired;
    private int level = 0;
    private JsonFormNode items;
    private boolean editable = true;

    public static void initLevel(JsonFormNode node) {
        if (MapUtils.isNotEmpty(node.getProperties())) {
            node.getProperties().forEach((propertyName, property) -> {
                property.setParent(node);
                property.setName(propertyName);

                if (StringUtils.isNotEmpty(node.getName())) {
                    property.setLevel(node.getLevel() + 1);
                } else {
                    property.setLevel(0);
                }

                if (MapUtils.isNotEmpty(property.getProperties()) || Objects.nonNull(property.getItems())) {
                    initLevel(property);
                }
            });
        }

        if (Objects.nonNull(node.getItems())) {
            JsonFormNode property = node.getItems();
            property.setParent(node);
            property.setName("items");
            property.setEditable(false);

            if (StringUtils.isNotEmpty(node.getName())) {
                property.setLevel(node.getLevel() + 1);
            } else {
                property.setLevel(0);
            }

            initLevel(node.getItems());
        }
    }
}
