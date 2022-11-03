package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component  //声明此切面Aspect为一个组件,不属于某一层
//@Aspect     //该注解表示当前组件为AOP中的切面组件
public class AlphaAspect {
    /**
     * 定义切面中的切点Pointcut：作用在于将增强逻辑(通知)织入到哪些对象target的哪些位置,即指定需要增强的位置
     *      使用@Pointcut注解实现,方法名任意,使用execution表达式指定需要织入增强逻辑的位置
     *       此处表达式中的第一个*表示处理任意返回值 第二个*表示service包下的所有的类(业务组件)
     *       第三个*表示所有的service组件中的所有方法 (..)表示所有的参数,    此处的*也可以指定给定的方法和返回值作为需要增强的切点
     */
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {

    }

    /**
     * @Before前置通知: 为在给定的切点pointcut()中的连接点(需要增强的具体业务)调用前执行此增强逻辑
     */
    @Before("pointcut()")
    public void before() {
        System.out.println("before");
    }

    /**
     * @After后置通知: 为在给定的切点pointcut()中的连接点(需要增强的具体业务)调用后执行此增强逻辑
     */
    @After("pointcut()")
    public void after() {
        System.out.println("after");
    }

    /**
     * @AfterReturning   :在连接点(需要增强的具体业务)的返回值得到之后执行此增强逻辑
     */
    @AfterReturning("pointcut()")
    public void afterRetuning() {
        System.out.println("afterRetuning");
    }

    /**
     * @AfterThrowing    ；在连接点抛出异常之后织入增强逻辑
     */
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    /**
     * @Around  环绕通知:在连接点(需要增强的具体业务)执行之前和之后都织入增强逻辑
     * @param joinPoint     该参数即为连接点(织入增强逻辑(通知)的部位)
     * @return      该方法需要有返回值Object,即为需要增强的具体业务的返回值(也可能为空)
     * @throws Throwable
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around before");//在连接点之前执行增强逻辑
        Object obj = joinPoint.proceed();//通过连接点调用需要增强的具体业务(自动生成代理对象执行业务方法)
        System.out.println("around after");//在连接点之后执行增强逻辑
        return obj;//返回值
    }
}
