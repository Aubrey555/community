package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    @Autowired  //通过该组件注入当前帖子详情界面,每个评论/评论的回复  的赞的数量
    private LikeService likeService;

    @Autowired      //向redis中存储更新的帖子,从而计算帖子得分
    private RedisTemplate redisTemplate;
    @Autowired
    private EventProducer eventProducer;//生产者组件
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
        //触发发帖事件,将帖子传入ES服务器
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)    //当前事件的主题为:TOPIC_PUBLISH，表示为发帖,则加入到消息队列
                .setUserId(user.getId())    //得到当前发贴用户的id
                .setEntityType(ENTITY_TYPE_POST)    //实体类型为POST
                .setEntityId(post.getId()); //得到帖子的id
        eventProducer.fireEvent(event);     //触发事件,将事件加入到消息队列

        //计算帖子分数:将更新的帖子id放到redisKey键对应的Set集合中
        String redisKey = RedisKeyUtil.getPostScoreKey();//得到计算帖子评分对应的键
        redisTemplate.opsForSet().add(redisKey,post.getId());//存储帖子id到Redis的Set集合中(保证无重复存储数据,对应键为redisKey)


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
            //2.1 得到当前帖子的点赞总数(传入当前点赞对应的实体类,即帖子;以及该帖子对应的id)
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
            //2.2 将当前帖子的点赞总数加入请求域中
        model.addAttribute("likeCount",likeCount);
            //2.2 得到当前用户是否对当前帖子进行了点赞(传入当前用户,以及当前用户访问的帖子id,)
                //如果用户未登录,直接返回0,表示未登录(在discuss.js异步请求中进行处理)
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus",likeStatus);

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

                //点赞数量:得到当前评论的点赞总数(传入当前点赞对应的实体类及其id)
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);//将评论的点赞总数放入commentVo集合中
                //点赞状态:得到当前用户是否对当前评论进行了点赞(传入当前用户,以及当前用户点赞的评论id)
                //如果用户未登录,直接返回0,表示未登录(在discuss.js异步请求中进行处理)
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus",likeStatus);//将用户对当前评论的点赞状态放入commentVo集合中

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

                        //点赞数量:得到当前回复的点赞总数(传入当前点赞对应的实体类及其id)
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);//将评论的点赞总数放入commentVo集合中
                        //点赞状态:得到当前用户是否对当前评论进行了点赞(传入当前用户,以及当前用户点赞的评论id)
                        //如果用户未登录,直接返回0,表示未登录(在discuss.js异步请求中进行处理)
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus",likeStatus);//将用户对当前评论的点赞状态放入commentVo集合中

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

    /**
     * 处理帖子置顶请求:为异步请求,即页面点击后不进行整体刷新,而是局部进行更新
     * @param id    传回帖子id
     * @return
     */
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody       //返回JSON类型字符串:响应状态码返回,表示是否置顶成功
    public String setTop(int id) {
        //1.更新帖子类型为1,表示置顶
        discussPostService.updateType(id, 1);

        //2.帖子发生变化,就需要将最新的帖子数据同步到ElasticSearch搜索引擎中,即触发发帖事件(将新的帖子进行更新)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())    //当前用户
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);//响应状态码0返回,表示置顶成功
    }

    /**
     * 处理帖子加精请求
     * @param id    帖子id
     * @return
     */
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        //1.帖子加精请求:状态为0为正常;1为加精;2为拉黑(删除)
        discussPostService.updateStatus(id, 1);

        //2.此时帖子也发生变化,因此触发发帖事件,将最新的帖子数据同步到ElasticSearch搜索引擎中
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

         //计算帖子分数::将更新的帖子id放到redisKey键对应的Set集合中
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }

    /**
     * 处理帖子删除请求
     * @param id   删除帖子的id
     * @return
     */
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        //1.帖子删除请求。状态为0为正常;1为加精;2为拉黑(删除)
        discussPostService.updateStatus(id, 2);

        //2.触发删帖事件,此时不再是将帖子更新到ES中,而需要将帖子从ES中进行删除(即出发删帖事件)
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        //3.返回删除成功代码
        return CommunityUtil.getJSONString(0);
    }

}
