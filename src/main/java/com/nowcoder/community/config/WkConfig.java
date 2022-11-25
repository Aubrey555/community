package com.nowcoder.community.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration  //该配置类用于在项目启动时自动创建文件夹,存放网页界面生成的图片文件(存在则不创建,不存在则创建)
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("E:/work/data/wk-images")
    private String wkImageStorage;//需要创建的存放生成图片文件的路径

    @PostConstruct  //由该注解修饰的方法,会在构造器初始化结束后执行,此处即在SpringBoot项目启动后进行执行
    public void init() {
        // 创建WK插件的图片目录
        File file = new File(wkImageStorage);
        if (!file.exists()) {
            file.mkdir();
            logger.info("创建WK图片目录: " + wkImageStorage);
        }
        //pdf目录不用创建,此项目不生成pdf
    }
}
