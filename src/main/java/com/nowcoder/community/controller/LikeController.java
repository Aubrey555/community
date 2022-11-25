package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired  //通过该组件实现点赞的业务逻辑
    private LikeService likeService;

    @Autowired    //得到当前请求的用户
    private HostHolder hostHolder;

    @Autowired  //注入EventProducer组件,用于在点赞事件被触发后,生产者组件被调用,向消息队列中发送事件对象Event
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 处理异步请求:点赞功能+点赞数量统计
     * @param entityType    对某一个实体(帖子/评论/评论的回复)进行点赞
     * @param entityId      该实体对应的id
     * @param entityUserId  当前实体对应的用户(帖子/评论/评论的回复对应的用户),该实体的帖子每得到一次点赞,则实体对应用户的总点赞数+1(个人主页进行显示)
     * @param postId        对当前点赞帖子的id
     * @return
     */
    @LoginRequired  //自定义注解LoginRequired,表示当前方法时候登录才能访问(标注此注解后,只有登录才能访问被标注的控制器方法)
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId,int postId) {
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

        //6.实现生产者发送点赞通知:    在某用户进行点赞(对帖子/评论/评论的回复)后,触发点赞事件,生产者发布消息到消息队列的指定主题中
            //6.1 该方法实现；第一次进行点赞;第二次点击则取消点赞。因此生产者向系统发送通知,只有在第一次进行点赞时再将通知发送到消息队列(取消点赞不进行通知)
        if(likeStatus == 1){    //此时表示当前用户userId对当前实体entityType的点赞状态为: 点赞事件触发的状态
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)     //此时为点赞事件,因此发布到点赞主题(服务器启动后,kafka会自动创建主题)
                    .setUserId(hostHolder.getUser().getId())    //触发事件的用户,即当前进行点赞的人
                    .setEntityType(entityType)    //触发事件用户操作的实体类型(即此时点赞的是帖子 还是 评论)
                    .setEntityId(entityId)      //实体类型对应的主键id
                    .setData("postId",postId);     //传入当前帖子的id(需要在系统通知界面,点击一个查看详情链接, 返回到此帖子所在界面)
            //6.2 调用生产者组件,将触发事件event发送到消息队列中
            eventProducer.fireEvent(event);
        }

        if(entityType == ENTITY_TYPE_POST){
            //此时表示对帖子进行点赞,则需要计算帖子的分数,则将帖子id加入redis中,定时任务下进行计算
            //计算帖子分数::将更新的帖子id放到redisKey键对应的Set集合中
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        //7.当前方法处理ajax异步请求,因此向页面返回JSON数据(响应编码  提示信息 业务数据)
        return CommunityUtil.getJSONString(0, null, map);//code返回0表示操作成功,会将此信息返回给ajax请求
    }
}
