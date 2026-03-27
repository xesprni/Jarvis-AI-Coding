package com.qihoo.finance.lowcode.common.util;

import lombok.Data;

/**
 * InnerCache
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote InnerCache
 */
@Data
public class InnerCache {
    private long seconds;
    private long cacheTime;
    private String key;
    private String value;
}
