package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController {

    @Autowired  //通过该组件实现点赞的业务逻辑
    private LikeService likeService;

    @Autowired    //得到当前请求的用户
    private HostHolder hostHolder;

    /**
     * 处理异步请求:点赞功能+点赞数量统计
     * @param entityType    对某一个实体(帖子/评论/评论的回复)进行点赞
     * @param entityId      该实体对应的id
     * @param entityUserId  当前实体对应的用户(帖子/评论/评论的回复对应的用户),该实体的帖子每得到一次点赞,则实体对应用户的总点赞数+1(个人主页进行显示)
     * @return
     */
    @LoginRequired  //自定义注解LoginRequired,表示当前方法时候登录才能访问(标注此注解后,只有登录才能访问被标注的控制器方法)
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId) {
        //1.获取当前用户
        User user = hostHolder.getUser();
        //2.当前用户对当前实体类的id进行点赞,并且实体类(帖子/评论/评论的回复)对应用户的总点赞数+1
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //3.统计当前实体的点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //4.查询当前用户userId对当前实体entityType的点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        //5.封装返回的结果到map,在页面对异步请求进行实现
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);//当前实体的点赞数量
        map.put("likeStatus", likeStatus);//当前用户userId对当前实体entityType的点赞状态
        //6.当前方法处理ajax异步请求,因此向页面返回JSON数据(响应编码  提示信息 业务数据)
        return CommunityUtil.getJSONString(0, null, map);//code返回0表示操作成功,会将此信息返回给ajax请求
    }
}
