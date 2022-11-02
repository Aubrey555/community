package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import java.util.List;

@Service    //service层,此时只是处理简单逻辑,但后面可能会进行过滤等操作
public class DiscussPostService {
    @Resource   //注入DiscussPostMapper接口
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;//注入组件

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

    /**
     * 保存发布的帖子到数据库
     *      1.转义文本中可能含有的HTML网页标签(比如如果帖子中有<script>xxx</script>标签,需要浏览器认为这是一个文本,而不是网页格式,以免对浏览器损害)
     *          1.1 使用Spring自带工具HtmlUtils,该工具会将给定文本中的特殊字符(标签等)转译为普通文本,而不进行解析
     *      2.对帖子的标题title和内容content进行过滤
     * @param post  帖子内容
     * @return
     */
    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        // 1.转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 2.过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        return discussPostMapper.insertDiscussPost(post);
    }

    /**
     * 通过帖子id查询帖子信息
     * @param id
     * @return
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 更新当前帖子评论的总数
     * @param id    帖子id
     * @param commentCount  帖子评论的总数
     * @return
     */
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

}
