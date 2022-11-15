package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired  //通过该组件得到关注/取关的功能
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired  //注入EventProducer组件,用于在关注事件被触发后,生产者组件被调用,向消息队列中发送事件对象Event
    private EventProducer eventProducer;

    /**
     * 实现关注功能
     *      浏览器发送异步请求,控制方法进行响应
     * @param entityType    当前登录用户需要关注的实体对象
     * @param entityId      实体对象的id
     * @return
     */
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();//获取当前用户
        //1.实现关注功能
        followService.follow(user.getId(), entityType, entityId);

        //2.实现生产者发送关注通知:    在某用户进行关注后,触发关注事件,生产者发布消息到消息队列的指定主题中
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)     //此时为关注事件,因此发布到关注主题(服务器启动后,kafka会自动创建主题)
                .setUserId(hostHolder.getUser().getId())    //触发事件的用户,即当前进行关注的人
                .setEntityType(entityType)    //触发事件用户操作的实体类型(即此时关注的对象,当前业务下只对用户进行关注)
                .setEntityId(entityId)      //实体类型对应的主键id
                .setEntityUserId(entityId);     //当前业务下只能对用户进行关注,因此EntityUserId即为实体类型的id,即为用户id即可(都唯一)
            //此时前端页面点击关注的通知,跳转到的是当前关注人员的主页,即为hostHolder.getUser().getId()即可(即A关注B,B点击通知,跳转到的是A的主页)
            //调用生产者组件,将触发事件event发送到消息队列中
        eventProducer.fireEvent(event);

        //3.关注成功,返回json对象(异步请求)
        return CommunityUtil.getJSONString(0, "已关注!");
    }

    /**
     * 当前用户实现取消关注
     * @param entityType    当前登录用户需要取消关注的实体对象
     * @param entityId
     * @return
     */
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();//得到当前用户
        followService.unfollow(user.getId(), entityType, entityId);//调用取消逻辑
        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    /**
     * 得到用户userId所有关注的人员列表
     * @param userId    路径中封装参数,使用@PathVariable获取(非当前用户)
     * @param page      分页数据
     * @param model     请求域数据
     * @return
     */
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        //1.通过传入的userId获得当前用户
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //2.前端页面传入当前需要查询的user用户,需要展示user用户的姓名
        model.addAttribute("user", user);
        //3.设置分页数据
        page.setLimit(5);//每页显示五条
        page.setPath("/followees/" + userId);//分页导航的超链接
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));//查询总数据条数,从而得到数据页数
        //4.得到该用户userId关注的用户列表
        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        //5.遍历userList,得到当前请求用户对于userId关注的用户user是否进行关注
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                ////5.1 将当前请求用户对于user的关注状态加入到map中(界面上根据此状态显示已关注/未关注)
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        //6.将此时的userList加入到请求域中
        model.addAttribute("users", userList);
        return "/site/followee";//访问关注用户界面
    }

    /**
     * 得到用户userId所有的粉丝列表
     * @param userId    查询的userId
     * @param page      分页所需数据
     * @param model     请求域数据
     * @return
     */
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        //1.根据userId得到用户对象
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        //2.分页所需数据
        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));
        //3.得到粉丝列表数据
        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        //4.得到当前登录用户是否关注此粉丝,得到关注的状态加入map中
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "/site/follower";
    }

    /**
     * 判断当前请求用户对于userId用户是否关注
     * @param userId
     * @return
     */
    private boolean hasFollowed(int userId) {
        //1.为空表示当前用户未登录
        if (hostHolder.getUser() == null) {
            return false;
        }
        //2.得到当前登录用户对于userId的关注状态
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
