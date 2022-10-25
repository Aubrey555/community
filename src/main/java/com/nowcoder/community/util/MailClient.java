package com.nowcoder.community.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/23 18:50
 * @Description: 该类表示邮箱客户端:提供发邮件功能(将发邮件的功能委托给新浪完成,即类似于客户端)
 */
@Component  //通用bean(在任何层次都可使用)
@Slf4j  //日志组件
public class MailClient {
    @Autowired
    private JavaMailSender mailSender;//Springmail核心接口,用于发送邮件
    //定义发送方(服务器中固定,即为配置的username);接收方;标题;内容等
    @Value("${spring.mail.username}")   //从配置文件中获得发送方
    private String from;//表示发送方(服务器在配置文件中指定),固定,都是通过服务器代理完成发送邮件

    /**
     * 完成发送邮件功能:调用该方法后,通过访问新浪服务器,将邮件发送给用户to,发送方即为配置文件中设置的spring.mail.username
     * @param to    邮件接收方
     * @param subject   邮件标题
     * @param content   邮件内容
     */
    public void sendMail(String to,String subject,String content){
        try {
            //1.构建JavaMailSender接口的核心属性MimeMessage,完成发送邮件方法调用
            MimeMessage message = mailSender.createMimeMessage();//创建MimeMessage模板对象(为空)
            //利用MimeMessageHelper帮助类,构建MimeMessage属性的具体内容
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);//传入发件人
            helper.setTo(to);//传入发件人
            helper.setSubject(subject);//传入邮件标题
            helper.setText(content,true);//传入邮件内容,设置为true,表示支持html格式内容
            mailSender.send(helper.getMimeMessage());//调用send()方法发送邮件(并且Message对象是从helper中获得)
        } catch (MessagingException e) {
            log.error("发送邮件失败: "+e.getMessage());
        }
    }
}
