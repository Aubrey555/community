package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired  //注入EventProducer组件,用于在评论事件被触发后,生产者组件被调用,向消息队列中发送事件对象Event
    private EventProducer eventProducer;
    @Autowired
    private DiscussPostService discussPostService;
    /**
     * 实现添加评论功能
     * @param discussPostId     当前帖子的id:添加评论后需要返回到当前帖子的详情界面进行展示(而重定向到帖子的详情界面需要携带当前帖子的id,因此该添加评论的路径中携带此id)
     * @param comment           表单页面提交评论数据(页面主要提交评论内容,隐含传入entityType(当前评论的目标实体,是帖子 还是帖子的评论)
     *                                              entityId(评论目标实体的id,比如评论一个帖子,该帖子的id,评论一个评论x,x的id)
     *                                              targetId,如果为一个帖子的评论x进行回复,该值记录发布此评论x的用户id)
     * @return
     */
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);//默认为0,表示此评论有效
        comment.setCreateTime(new Date());
        commentService.addComment(comment);//添加此评论

        //实现生产者发送评论通知:    在某用户发布评论后,触发评论事件,生产者发布消息到消息队列的指定主题中
            //1.创造评论对象,并进行赋值
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)    //此时为评论事件,因此发布到评论主题(服务器启动后,kafka会自动创建主题)
                .setUserId(hostHolder.getUser().getId())    //触发事件的用户,即当前发布评论的人
                .setEntityType(comment.getEntityType())        //触发事件用户操作的实体类型(即评论的是帖子, 还是评论了 一条评论)
                .setEntityId(comment.getEntityId())            //该实体类型对应的id
                .setData("postId",discussPostId);              //传入当前帖子的id(需要在系统通知界面,点击一个查看详情链接, 返回到此帖子所在界面)
            //而对于Event类型的entityUserId属性; 即该实体对应的用户(即帖子/评论/用户 对应的人的id),需要根据当前评论的实体类型EntityType进行查询
        if(comment.getEntityType() == ENTITY_TYPE_POST){//即如果当前评论的实体类型为帖子,则entityUserId 应为 帖子对应的userId
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());//得到帖子对应的实体类(即为此时评论的目标)
            event.setEntityUserId(target.getUserId());//得到该帖子的用户id(即发布此帖子的人)
        }else if(comment.getEntityType() == ENTITY_TYPE_COMMENT){ //此时即当前评论的实体类型为 一个评论,则entityUserId 应为 此评论对应的userId
            Comment target = commentService.findCommentById(comment.getEntityId());//此时即通过comment.getEntityId()得到评论对应的实体类(即target为当前评论的目标,也是一个评论)
            event.setEntityUserId(target.getUserId());  //存储此评论的userId
        }
            //2.调用生产者组件,将触发事件event发送到消息队列中
        eventProducer.fireEvent(event);

        //3.如果当前评论为评论帖子时,则触发帖子事件,将当前帖子提交到ES服务器中
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            //触发帖子事件,加入到ES服务器中
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)    //当前事件的主题为:TOPIC_PUBLISH，表示为发帖,则加入到消息队列
                    .setUserId(comment.getUserId())    //得到当前发贴用户的id
                    .setEntityType(ENTITY_TYPE_POST)    //实体类型为POST
                    .setEntityId(discussPostId); //得到帖子的id
            eventProducer.fireEvent(event);     //触发事件,将事件加入到消息队列
        }
        return "redirect:/discuss/detail/" + discussPostId;//重定向到帖子的详情界面
    }

}
