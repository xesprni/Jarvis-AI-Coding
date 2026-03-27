package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.gentracker.tool.Md5Utils;

import java.util.Map;
import java.util.TreeMap;

public class CryptoUtil {

    public static String getMd5(Map<String, String> map) {
        // 1. 使用 TreeMap 保证 Map 的顺序
        Map<String, String> sortedMap = new TreeMap<>(map);
        // 2. 将 Map 转换为字符串形式
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        // 去掉最后一个 "&"
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        return Md5Utils.md5Digest(sb.toString());
    }
}
