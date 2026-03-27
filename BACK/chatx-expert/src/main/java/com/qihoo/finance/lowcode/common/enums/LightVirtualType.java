package com.qihoo.finance.lowcode.common.enums;

import lombok.Getter;

/**
 * LightVirtualType
 *
 * @author fengjinfu-jk
 * date 2024/1/25
 * @version 1.0.0
 * @apiNote LightVirtualType
 */
@Getter
public enum LightVirtualType {
    JAVA(".java"),
    XML(".xml"),
    FTL(".ftl"),
    JSON(".json"),
    VM(".java.vm"),
    MARKDOWN(".md"),
    SQL(".sql"),
    ;

    private final String value;

    LightVirtualType(String value) {
        this.value = value;
    }
}
