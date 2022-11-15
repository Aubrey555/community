package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class TransactionTests {

    @Autowired
    private AlphaService alphaService;

    //测试声明式事务(注解方式)逻辑
    @Test
    public void testSave1() {
        Object obj = alphaService.save1();
        System.out.println(obj);
    }

    @Test
    public void testSave2() {
        Object obj = alphaService.save2();
        System.out.println(obj);
    }

}
