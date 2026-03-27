package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.common.exception.AssertException;
import org.apache.commons.lang3.StringUtils;

public class AssertUtil {

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertException(message);
        }
    }

    public static void notBlank(String obj, String message) {
        if (StringUtils.isBlank(obj)) {
            throw new AssertException(message);
        }
    }

    public static void assertTrue(Boolean value, String message) {
        if (!Boolean.TRUE.equals(value)) {
            throw new AssertException(message);
        }
    }

}
