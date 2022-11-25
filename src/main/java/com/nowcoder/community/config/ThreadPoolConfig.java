package com.nowcoder.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration  //表明该类为一个配置类组件
@EnableScheduling   //该注解表明启用定时任务线程池
@EnableAsync        //该注解表明可以使用@Async注解作用方法,是方法为一个线程体,使用Spring普通线程池去执行
public class ThreadPoolConfig { //Spring线程池所需的配置类
}
