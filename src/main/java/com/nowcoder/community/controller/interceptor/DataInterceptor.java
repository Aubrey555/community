package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component      //该拦截器组件用于处理所有请求,记录UV(独立访客)和DAU(日活用户)
public class DataInterceptor implements HandlerInterceptor {//表示为一个拦截器

    @Autowired
    private DataService dataService;//业务逻辑层组件:用于进行网站数据统计,比如UV(独立访客)和DAU(日活用户)

    @Autowired
    private HostHolder hostHolder;//持有本次登录对应的用户id

    @Override       //preHandle()在控制器执行之前就进行统计UV和DAU数据,不对请求进行拦截
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.统计UV
        String ip = request.getRemoteHost();//得到当前请求对应的ip地址
        dataService.recordUV(ip);//记录本次请求对应的ip地址

        //2.统计DAU
        User user = hostHolder.getUser();//得到当前登录用户
        if (user != null) {//用户不为空
            dataService.recordDAU(user.getId());//统计此日活用户信息
        }
        //3.放行请求
        return true;
    }
}
