package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

//@Mapper //已经在主类进行所有mapper接口的扫描,此处可以不用写该注解
public interface CommentMapper {
    /**
     * 根据实体entityType查询所有的评论(即查询帖子的评论,还是评论的评论,还是视频课程的评论等)
     * @param entityType    实体类型,表示评论的目标和内容(1:帖子 2:评论 3:用户 4:题目 5:课程视频)
     * @param entityId      表示当前评论的目标是哪个帖子(映射帖子的id)
     * @param offset        分页数据:当前页的起始行,即表示当前页的起始行应该为第几条数据
     * @param limit         分页每页显示数据
     * @return
     */
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    /**
     * 根据实体entityType查询当前评论的总数
     * @param entityType    表示评论的目标和内容
     * @param entityId      表示当前评论的目标是哪个帖子
     * @return
     */
    int selectCountByEntity(int entityType, int entityId);
    //插入一条评论
    int insertComment(Comment comment);
}
