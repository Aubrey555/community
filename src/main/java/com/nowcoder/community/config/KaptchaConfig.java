package com.nowcoder.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

//该配置类用于生成用户登陆的验证码
@Configuration  //表示该类为配置类
public class KaptchaConfig {

    //kaptcha组件核心的对象为一个接口Producer,其中createText()方法用于创建验证码文字;createImage()通过文字创建验证码图片.
    //该接口默认的实现类为DefaultKaptcha,因此需要在此配置类中,配置该接口的实现类对象的相关属性
    @Bean   //将此组件加入到容器中
    public Producer kaptchaProducer() {
        Properties properties = new Properties();//该对象中加入Kaptcha组件实例化的数据
        properties.setProperty("kaptcha.image.width", "100");//验证码图片的宽度(像素)
        properties.setProperty("kaptcha.image.height", "40");//验证码图片的高度
        properties.setProperty("kaptcha.textproducer.font.size", "32");//验证码字体
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");//字体颜色(表示黑色)
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYAZ屈志峰");//验证码随机字符的可以匹配的范围
        properties.setProperty("kaptcha.textproducer.char.length", "4");//验证码生成随机字符的长度
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");//采用的噪声类(即对于图片的处理)

        DefaultKaptcha kaptcha = new DefaultKaptcha();//Kaptcha组件的实现类对象DefaultKaptcha
        Config config = new Config(properties);//通过Config对象对参数进行配置;该对象需要传入Properties对象
        kaptcha.setConfig(config);//参数配置
        return kaptcha;
    }
}
