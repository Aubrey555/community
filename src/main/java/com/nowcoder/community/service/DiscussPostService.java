package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service    //service层,此时只是处理简单逻辑,但后面可能会进行过滤等操作
public class DiscussPostService {
    @Resource   //注入DiscussPostMapper接口
    private DiscussPostMapper discussPostMapper;

    /**
     * 分页方式返回当前用户的所有帖子
     * @param userId    用户id,如果为0则表示查询所有用户
     * @param offset    分页所需数据:当前页的页码
     * @param limit     分页所需数据:每页显示条数
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }
    /**
     * 通过用户id查询该用户的帖子总数(如果为0表示所有用户)
     * @param userId        用户的id
     * @return
     */
    public int findDiscussPostRows(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }
}
