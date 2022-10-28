package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)       //表示该注解用于方法(拦截器就是拦截控制器方法)
@Retention(RetentionPolicy.RUNTIME) //表示运行时有效
public @interface LoginRequired {   //自定义注解LoginRequired,表示当前方法时候登录才能访问(标注此注解后,只有登录才能访问被标注的控制器方法)

}
