package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component  //自定义拦截器:用于处理@LoginRequired注解,对于标注了该注解的方法,只有当前已经登录才能访问,否则直接进行拦截
public class LoginRequiredInterceptor implements HandlerInterceptor {//用于验证用户登录状态的拦截器

    @Autowired
    private HostHolder hostHolder;//通过该组件获得当前请求持有的用户

    @Override   //处理请求之前就对当前登录状态进行判断(只有已经登陆才进行放行)
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {//handler表示处理当前请求的控制器,如果拦截到的控制器是一个请求方法则进行处理(对于静态资源等处理器不进行拦截)
            //1.转型,将handler转型为请求方法处理器
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //2.获得当前请求方法处理器要处理的方法(拦截器判断是否对其进行拦截)
            Method method = handlerMethod.getMethod();
            //3.得到该方法上是否存在LoginRequired类型的注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            //4.如果该对象不为空,表示方法上标注了此注解,并且当前请求持有的用户为空, 则表示没有登陆, 则对该请求进行拦截
            if (loginRequired != null && hostHolder.getUser() == null) {
                //5. 则请求重定向到登陆界面
                response.sendRedirect(request.getContextPath() + "/login");
                return false;//此时表示用户并未登录,拦截当前请求,不予放行
            }
        }
        return true;//表示用户已经登陆,请求放行
    }
}
