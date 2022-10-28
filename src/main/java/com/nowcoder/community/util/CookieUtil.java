package com.nowcoder.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

//该工具类实现从Cookie中得到指定name的值
public class CookieUtil {

    //从当前请求中的Cookie内得到属性为name对应的值(静态方法,直接方法名进行调用,不使用容器)
    public static String getValue(HttpServletRequest request, String name) {
        if (request == null || name == null) {
            throw new IllegalArgumentException("参数为空!");
        }
        //得到当前请求所有的Cookie数组
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();//得到当前cookie的value值
                }
            }
        }
        return null;
    }

}
