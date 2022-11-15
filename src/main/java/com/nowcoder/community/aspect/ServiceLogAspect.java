package com.nowcoder.community.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
@Slf4j
public class ServiceLogAspect {
    //private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    /**
     * 声明切点:作用在于将增强逻辑(通知)织入到哪些对象target的哪些位置,即指定需要增强的位置
     *     即指定所有的业务组件都需要记录日志
     */
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {
    }

    /**
     * @Before 前置通知: 为在给定的切点pointcut()中的连接点(需要增强的具体业务)调用前执行此增强逻辑
     *      日志格式；(ip地址)用户[1.2.3.4],在[xxx],访问了[com.nowcoder.community.service.xxx()].
     * @param joinPoint     连接点参数,指代将要调用的业务逻辑方法
     */
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        //1.通过工具类RequestContextHolder获取其子类型ServletRequestAttributes对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null){     //此时调用service业务的可能不是控制层业务(kafka实现消息队列中,使用了生产者消费者也调用了业务曾组件),因此不存在请求,attrbiutes为空
            return;     //此时不记录日志,直接退出即可
        }
        //2.通过attributes得到当前请求的request对象
        HttpServletRequest request = attributes.getRequest();
        //3.得到当前请求的ip地址
        String ip = request.getRemoteHost();
        //4.得到当前时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //5.通过连接点(将要调用的业务逻辑方法),得到方法的类名getDeclaringTypeName() ,以及调用的方法名getName()
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        //6.拼接日志进行记录
        log.info(String.format("用户[%s],在[%s],访问了[%s].", ip, now, target));
    }
}
