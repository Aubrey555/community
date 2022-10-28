package com.nowcoder.community.controller.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component  //创建组件,交由Spring容器进行管理
@Slf4j      //日志注解
public class AlphaInterceptor implements HandlerInterceptor {//实现接口,表示该类为一个拦截器

    // 该方法在Controller之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.debug("preHandle()方法被执行: " + handler.toString());//日志输出
        return true;//返回true表示对该请求不进行拦截,程序继续执行(拦截请求,不进行controller)
    }

    // 该方法在Controller之后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.debug("postHandle()方法被执行: " + handler.toString());
    }

    // 该方法在TemplateEngine(页面渲染)之后执行,也就是先进性页面渲染,再执行此方法
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.debug("afterCompletion()方法被执行: " + handler.toString());
    }
}
