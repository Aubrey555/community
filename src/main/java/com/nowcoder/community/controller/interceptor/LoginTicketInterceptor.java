package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component      //将该组件加入容器中
public class LoginTicketInterceptor implements HandlerInterceptor {
//显示用户登陆信息的拦截器
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;//注入线程容器,用于在本次请求中持有该User对象

    // 在请求开始时查询登录用户,并在此次请求中一直持有用户
                // (浏览器只要登陆成功后,一访问服务器,都会携带含有凭证cookie的ticket,因此可以在服务端获取此ticket对应的用户,在本次请求中,一直存储该用户)
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.从cookie中获取凭证 (工具类CookieUtil中封装方法,从本次请求request的Cookie中得到name对应的值)
        String ticket = CookieUtil.getValue(request, "ticket");
        //2.通过凭证ticket查询对应的用户,并在此次请求中持有该用户
        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效(凭证有效  并且   超时时间未过期)
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // 根据凭证查询用户(loginTicket.getUserId()得到此用户对应的id)
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户(在后面使用,此处直接存入即可)
                    //此处即将User数据存储在当前线程对应的map集合中,只要当前请求处理未结束,则线程一直存在,请求处理结束,服务器做出响应,则该线程被销毁
                hostHolder.setUser(user);

                //构建用户认证的结果,并存入SercurityContext中。以便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                //存入SecurityContext中
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

            }
        }
        return true;//放行请求
    }

    //在模板引擎渲染之前就将User对象存储到Model中
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();//得到当前请求(线程)持有的User
        if (user != null && modelAndView != null) {//并且存储到Model中
            modelAndView.addObject("loginUser", user);
        }
    }

    //模板引擎渲染结束后,将持有的User对象进行清空
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();//清除用户认证信息
    }
}
