package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

/**
 * 工具类:用于账号注册业务
 */
public class CommunityUtil {
    // 生成随机字符串(用于账号注册中的激活码,激活码等)
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");//将UUID中的 - 替换为空
    }
    // MD5算法对密码进行加密
    // hello -> abc123def456(对于hello的密码 每次加密结果可能都是该值,因此仍然存在对密码试出的可能)
    // hello + 3e4a8 -> abc123def456abc(对于用户给定的密码+随机生成的字符串,再进行MD5加密,则会很难破解)
    public static String md5(String key) {
        if (StringUtils.isBlank(key)) {//使用Commons lang包下的工具类StringUtils判定给定密码key是否为空(如果是null,空串等都为空)
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());//DigestUtils为spring自带工具,进行md5加密
    }
}
