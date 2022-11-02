package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 该控制器用于实现发布帖子以及帖子评论等相关内容
 */
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;//注入Service层组件

    @Autowired
    private HostHolder hostHolder;//得到当前请求拥有的用户User

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    /**
     * 实现发布帖子的功能
     * @param title     页面传入帖子标题
     * @param content   页面传入帖子内容
     * @return
     */
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        //1.得到当前请求所具有的用户
        User user = hostHolder.getUser();
        if (user == null) {
            //返回自定义提示码403(表示没有权限)信息,并以JSON格式的字符串响应到浏览器
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }
        //2.创建帖子对象
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());//发帖时间为当前时间
        //3.发布帖子(保存到服务器)
        discussPostService.addDiscussPost(post);
        //4.发布成功,返回响应码为0(报错的情况,将来统一处理.),返回json格式的字符串
        return CommunityUtil.getJSONString(0, "发布成功!");
    }

    /**
     * 根据帖子id查询帖子具体内容并进行分页显示评论内容
     * @param discussPostId     请求路径中携带帖子id
     * @param model             包含返回的信息
     * @param page              分页相关信息(对帖子评论的分页数据),只要是一个实体类型javabean,则springMVC会自动将此类型注入到Model中,从而在界面内可以获取
     * @return
     */
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        // 1.通过id查询帖子内容
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        // 2.帖子作者显示(即通过帖子id显示帖子作者)
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        // 3.评论的分页信息
        page.setLimit(5);//表示每页显示5条数据
        page.setPath("/discuss/detail/" + discussPostId);//设置分页的查询路径(即点击分页导航上的页码后跳转到的路径)+当前帖子的id,即跳转到当前页面
        page.setRows(post.getCommentCount());//设置分页中的帖子总数(用于获取页面数),通过当前帖子post的CommentCount属性获取

        // 4.获取当前实体类(帖子)的评论列表(entityType: 1:帖子 2:评论 3:用户 4:题目 5:课程视频)
        // 评论(ENTITY_TYPE_POST = 1): 给帖子的评论
        // 回复(ENTITY_TYPE_COMMENT = 2): 给评论的评论
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());//该帖子的所有评论

        // 5.所有评论VO(viewObject显示对象)集合commentVoList:即通过当前帖子的所有评论得到当前对应用户的姓名和头像
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {//遍历当前帖子(ENTITY_TYPE_POST)的每个评论
                //5.1 commentVo中封装当前评论的内容(当前用户的评论内容/当前用户)  评论VO
                Map<String, Object> commentVo = new HashMap<>();
                //   封装评论内容
                commentVo.put("comment", comment);
                //   封装评论作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                //5.2 得到当前评论(ENTITY_TYPE_COMMENT)的所有回复,封装到replyList回复列表中
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);//从第0行开始查,并显示所有回复
                        //使用replyVoList封装当前评论comment的回复的所有信息(回复VO列表)
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {//遍历当前评论(ENTITY_TYPE_COMMENT)的所有回复reply
                        //5.2.1 replyVo中封装当前评论的某个回复的用户信息
                        Map<String, Object> replyVo = new HashMap<>();
                        //      封装回复内容
                        replyVo.put("reply", reply);
                        //      封装回复的作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //      得到回复的目标(即当前回复是回复哪个用户的,可能为当前评论,也可能是其他回复),并进行封装
                        //每个评论的回复可能是直接回复该评论,也可能是回复该评论的一个回复
                        //(因此得到此回复的对象,如果是0表示回复评论,则使得target为空,否则不为0表示回复一个评论的回复,得到该评论回复的对象)
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        //replyVoList中加入当前评论的所有回复
                        replyVoList.add(replyVo);
                    }
                }
                //5.3 commentVo中加入当该帖子的当前评论的所有回复
                commentVo.put("replys", replyVoList);

                //5.4 得到当前评论的所有回复的数量并加入
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                //5.5  封装commentVo的某个评论信息到commentVoList帖子的总评论集合

                commentVoList.add(commentVo);
            }
        }
        //6.封装所有评论到commentVoList并加载到请求域中(每个评论也含有自己的所有回复)
        model.addAttribute("comments", commentVoList);
        //7. 加载帖子详情模板界面
        return "/site/discuss-detail";
    }

}
