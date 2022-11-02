package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
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

    /**
     * 获取JSON格式的字符串:将传入的信息封装为JSON对象,再将JSON对象封装为JSON格式的字符串返回
     * @param code  编号(比如响应代码的编号)
     * @param msg   提示信息
     * @param map   封装业务数据
     * @return
     */
    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        //1.通过fastjson依赖,得到JSON对象
        JSONObject json = new JSONObject();
        //2.将传入数据封装到JSON对象中
        json.put("code", code);
        json.put("msg", msg);
        if (map != null) {
            for (String key : map.keySet()) {
                json.put(key, map.get(key));//将map中的每个键值对加入到json中
            }
        }
        //3.返回JSON格式的字符串
        return json.toJSONString();
    }
    //重载getJSONString方法:map为空
    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }
    //重载getJSONString方法:msg,map为空
    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }

    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhangsan");
        map.put("age", 25);
        System.out.println(getJSONString(0, "ok", map));
    }


}
