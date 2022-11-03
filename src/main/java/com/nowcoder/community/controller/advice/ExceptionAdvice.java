package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)//用于修饰类，表示该类是Controller的全局配置类。并且只扫描Controller组件
@Slf4j
public class ExceptionAdvice {
    /**
     * 该方法用于处理所有的异常情况,用于记录异常出现的日志。使用@ExceptionHandler完成
     *      该方法必须没有返回值,方法名任意取,并且可以携带多个参数,通常传入如下参数:
     * @param e     传入当前控制器抛出的异常
     * @param request   处理请求和响应
     * @param response
     * @throws IOException
     */
    @ExceptionHandler({Exception.class})    //用于修饰方法，该方法会在Controller出现异常后被调用，用于处理所有捕获到的异常Exception
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //1.当该方法被调用时,表明控制器发生异常,记录该异常信息到日志中
        log.error("服务器发生异常: " + e.getMessage());
        //2.记录异常栈中的所有信息(遍历e.getStackTrace())
        for (StackTraceElement element : e.getStackTrace()) {
            log.error(element.toString());//每一个element记录一条异常信息
        }
        //3.浏览器发送的普通请求可能是想返回到一个界面,因此如果发生异常可以返回错误的页面500.html
            //但也可能是一个ajax的异步请求,该请求是想返回一个json对象,因此需要进行区分
            //通过
        String xRequestedWith = request.getHeader("x-requested-with");//获取请求头中key对应的字符串
        if ("XMLHttpRequest".equals(xRequestedWith)) {//如果字符串==该值,则表示当前请求为异步请求
            //表示向浏览器返回一个普通字符串,字符串为json格式,浏览器中使用$.parseJSON方法将此字符串转化为js对象进行取值
            response.setContentType("application/plain;charset=utf-8");
            //获取字符输出流,向浏览器返回数据
            PrintWriter writer = response.getWriter();
            //输出json字符串,为服务器异常信息
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
