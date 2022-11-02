package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
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
        return "redirect:/discuss/detail/" + discussPostId;//重定向到帖子的详情界面
    }

}
