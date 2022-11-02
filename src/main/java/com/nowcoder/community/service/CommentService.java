package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostService discussPostService;
    // 通过实体类查询当前所有评论
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }
    //通过实体类及其id查询评论总数
    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    /**
     * 为帖子添加评论并更新帖子评论的总数(在一个事务内完成)
     *      isolation = Isolation.READ_COMMITTED声明事务隔离级别为读已提交  传播级别为:REQUIRED(表示支持当前事务(外部事务),如果外部事务不存在则创建新事务.)
     * @param comment
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        //1.添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));//过滤评论中的html标签(对其进行转义为文本,而不是标签内容)
        comment.setContent(sensitiveFilter.filter(comment.getContent()));//过滤评论中的敏感词
        int rows = commentMapper.insertComment(comment);//插入评论

        //2.更新帖子评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {//EntityType() == ENTITY_TYPE_POST,表示当前评论作用于帖子
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());//根据实体entityType查询当前评论的总数
            discussPostService.updateCommentCount(comment.getEntityId(), count);//更新帖子的评论总数(当前的帖子id=comment.getEntityId())
        }
        return rows;
    }

}
