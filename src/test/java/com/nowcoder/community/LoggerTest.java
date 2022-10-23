package com.nowcoder.community;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/23 15:19
 * @Description: 该测试类用于测试日志信息
 */
@SpringBootTest
@Slf4j  //方式1:使用该注解创建Logger对象,默认id=log
public class LoggerTest {
    //方式2:或者直接实例化Logger,创建该类的日志信息对象(此时即不需要@Slf4j注解)
    private static final Logger logger = LoggerFactory.getLogger(LoggerTest.class);
    //测试Logger
    @Test
    public void testLogger(){
        System.out.println(logger.getName());//输出实例化创建的Logger对象的名称
        System.out.println(log.getName());

        //使用方式1创建的日志对象打印日志信息:
        log.debug("debug log");
        log.info("info log");
        log.warn("warn log");
        log.error("error log");
        System.out.println("========分隔符========");
        //使用方式2创建的日志对象打印日志信息:
        logger.debug("debug log");
        logger.info("info log");
        logger.warn("warn log");
        logger.error("error log");
    }
}
