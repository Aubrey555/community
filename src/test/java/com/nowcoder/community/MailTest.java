package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/23 19:06
 * @Description: 测试发送邮件功能
 */
@SpringBootTest
public class MailTest {
    @Autowired
    private MailClient mailClient;
    @Test   //测试发送普通文本格式邮件
    public void testTextMail(){
        mailClient.sendMail("2379915026@qq.com","测试发送邮件的功能","呕吼,起飞！");
    }
    @Autowired      //获得Thymeleaf的模板引擎,用于生成Html格式的网页内容(html格式一般会经过渲染,此处的模板只是简单处理),从而调用MailClient工具类的发送方法
    private TemplateEngine templateEngine;

    @Test   //测试发送HTML格式(html模板)的邮件
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","赵莉!");//将模板需要的变量在此传入
        //调用模板引擎,生成动态网页(HTML格式):传入模板位置+模板需要数据
        String content = templateEngine.process("mail/demo", context);
        System.out.println(content);//输出该动态网页

        mailClient.sendMail("2516324596@qq.com","亲爱的~",content);
    }

}
