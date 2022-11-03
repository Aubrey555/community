package com.nowcoder.community.dao;


import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/19 17:19
 * @Description:
 */
//@Mapper
public interface DiscussPostMapper {
    //通过用户id查询该用户的帖子总数(@Param注解中的属性表示为当前参数的userId的别名)
    //如果该方法只有一个参数,并且在动态sql<if>中使用,则必须使用该注解加上别名(userId为0表示直接从表中搜索)
    int selectDiscussPostRows(@Param("userId") int userId);
    /**
     * 分页显示: 通过用户id返回该用户的帖子(个人主页的帖子)
     * @param userId    用户id(如果用户id为0则表示直接从表中搜索)
     * @param offset    分页所需数据:当前页的页码
     * @param limit     分页所需数据:每页显示条数
     * @return
     */
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);
    //插入一个帖子
    int insertDiscussPost(DiscussPost discussPost);
    //通过id得到帖子的详细信息
    DiscussPost selectDiscussPostById(int id);
    //更新当前帖子的评论数量
    int updateCommentCount(int id,int commentCount);

}
