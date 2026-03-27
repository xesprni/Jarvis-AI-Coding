package com.qihoo.finance.lowcode.gentracker.tool;

import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密
 *
 * @author fengjinfu-jk
 * date 2023/8/2
 * @version 1.0.0
 * @apiNote Md5Utils
 */
public class Md5Utils {

    public static String md5Digest(String input) {
        return StringUtils.defaultIfEmpty(md5(input), "").toUpperCase();
    }

    private static String md5(String input) {
        if (input == null || input.length() == 0) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] byteArray = md5.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : byteArray) {
                // 一个byte格式化成两位的16进制，不足两位高位补零
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
