package com.nowcoder.community.controller;

import com.nowcoder.community.util.CommunityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/25 15:52
 * @Description: 该控制器用于测试Cookie/Session使用
 */
@Controller
@RequestMapping("/test")    //表示该控制器的根路径,访问此控制器的请求路径时,必须携带该方法
public class TestController {

    //cookie示例:模拟浏览器第一次访问服务器,服务器创建Cookie对象,并通过响应体将其发送给浏览器,浏览器第二次访问服务器会携带该Cookie对象
    //1.服务器响应浏览器的请求,Cookie包含在响应体中,与返回的对象为JSON还是页面无关

    /**
     * 完成浏览器第一次访问服务器,服务器创建Cookie,并包含在响应体返回给浏览器
     * @return
     */
    @GetMapping("/cookie/set")
    @ResponseBody //返回一个json字符串,而不是跳转到页面
    public String setCookie(HttpServletResponse response){
        //1.创建Cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        //2.设置cookie生效范围(即浏览器得到该cookie后,下一次访问服务器的那些路径才携带该cookie对象,如果不设置,则浏览器访问服务器的任何路径都携带此Cookie,会造成资源浪费)
        cookie.setPath("/community/test");//表示访问该项目/community下的/test请求及其子路径才有效(/community在配置文件中进行设置,/test为此控制器的根路径)
        //3.设置cookie的生存时间
            //(Cookie默认存储在浏览器内存中,一旦关闭浏览器此Cookie则消失,下一次打开浏览器访问请求则不会携带;如果设置生存时间,则即使关闭浏览器Cookie仍存在,除非超时)
        cookie.setMaxAge(60 * 10);//设置了生存时间,则Cookie会保存到硬盘中
        //4.发送cookie(即放到response中)
        response.addCookie(cookie);
        return "set cookie successful!";
    }

    /**
     * 完成浏览器第二次访问服务器,携带第一次服务器创建给浏览器的Cookie对象,从而识别为相同的浏览器
     * @param code
     * @return
     */
    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code) {//得到指定key对应的Cookie值
        System.out.println(code);
        return "get cookie successful!";
    }

    // session示例

    /**
     * 浏览器第一次访问服务器,服务器响应头携带包含sessionid的cookie发送到浏览器
     *      session保存在服务端,因此可以存入任意类型数据。每个session对象具有自己的唯一标识sessionid,服务器响应客户端时,会包含在cookie发送
     * @param session
     * @return
     */
    @RequestMapping(path = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session) {//SpringMVC会自动创建Session对象,并完成自动注入
        session.setAttribute("id", 1);//向session中存储数据
        session.setAttribute("name", "Test");
        return "set session";
    }

    /**
     * 浏览器访问当前请求:如果之前访问过服务器,并且向session中存储了数据,则当前浏览器的请求头中会包含Cookie对象,
     *      cookie中含有与session唯一对应的标识sessionid(每个浏览器都有自己的session与服务器对应)
     *      此处得到当前session中的对象
     * @param session
     * @return
     */
    @RequestMapping(path = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session) {
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }


}
