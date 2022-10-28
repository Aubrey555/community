package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.AlphaInterceptor;
import com.nowcoder.community.controller.interceptor.LoginRequiredInterceptor;
import com.nowcoder.community.controller.interceptor.LoginTicketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration  //配置类,此处对拦截器进行配置,需要实现WebMvcConfigurer接口,将拦截器添加到组件中
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor;//得到拦截器接口

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;//注入显示用户登陆信息的拦截器

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;  //获得用于验证用户登录状态的拦截器

    @Override       //注册拦截器接口
    public void addInterceptors(InterceptorRegistry registry) {
        //注册自定义的拦截器接口
        //测试拦截器
        //excludePathPatterns()方法中设置不需要拦截的路径(对于静态资源不需要进行拦截),例如:/**/*.css文件,记载根目录resources所有子目录下的css文件都进行排除
        //addPathPatterns()方法设置需要拦截的路径,即对于/register注册路径进行拦截,对登录请求/login进行拦截
        registry.addInterceptor(alphaInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")
                .addPathPatterns("/register", "/login");

        //对显示用户信息的拦截器进行注册,并对除了静态资源外的路径都进行拦截
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        //表示该拦截器排除对所有静态资源的拦截
        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
}
