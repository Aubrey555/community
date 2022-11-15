package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component  //该拦截器用户获取当前用户的所有未读消息数量,发送到请求域,在index.html的header标签进行显示
public class MessageInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;//获取当前用户

    @Autowired
    private MessageService messageService;//通知处理逻辑

    @Override   //调用
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {     //表示当前用户已经登陆
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);//查询所有的未读私信总数
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);//查询所有的未读通知总数
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);//向页面传输总的未读信息总数
        }
    }
}
