package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//该类表示SpringQuartz组件中的定时任务逻辑(实现job接口给出) :网站需要的实际定时任务,每隔一段时间则统计帖子分数,进行排行(对于redisKey中的所有帖子id进行排行)
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired      //需要得到需要计算分数的帖子id
    private RedisTemplate redisTemplate;

    @Autowired      //通过id查询帖子
    private DiscussPostService discussPostService;

    @Autowired      //可以查询某个帖子点赞的数量等
    private LikeService likeService;

    @Autowired      //将更新后的帖子更新到搜索引擎中
    private ElasticsearchService elasticsearchService;

    // 初始化常量数据:牛客纪元,即为项目上线的时间
    private static final Date epoch;
    //初始化项目时间
    static {
        try {
            //epoch即为初始化的项目上线时间
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!", e);
        }
    }

    /**
     * 定时任务:用于统计给定redisKey键对应的所有帖子id的分数score
     *      score = log(精华分 + 评论数*10 + 点赞数*2 + 收藏数*2) + (发布时间 – 牛客纪元,即初始化时间epoch)
     * @param context
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        //1.boundSetOps(redisKey)获得给定key的操作对象,该key对应的数据类型为Set,通过此操作对象可以对key对应集合的数据进行操作
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        //2.判断:即是否需要对帖子进行计分
        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");//日志记录
            return;
        }
        //3.刷新帖子分数
        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        while (operations.size() > 0) {     //只要redis中存在需要计分的帖子,就进行计分
            this.refresh((Integer) operations.pop());//弹出帖子id,用于刷新帖子分数(弹出后,则此key对应的Set集合减小)
        }
            //刷新结束后,redis中就不存在需要计分的帖子了
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    /**
     * 该方法用于刷新给定帖子的分数
     * @param postId    给定的帖子id
     */
    private void refresh(int postId) {
        //1.得到帖子
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        //2.帖子为空
        if (post == null) {
            logger.error("该帖子不存在: id = " + postId);
            return;
        }
        //3.得到帖子相关信息
            // 是否精华:通过精华给出权重比
        boolean wonderful = post.getStatus() == 1;
            // 评论数量
        int commentCount = post.getCommentCount();
            // 点赞数量:帖子类型+帖子id
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
            // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
            // 计算分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);//此时即换算为距离天数
        // 更新帖子分数
        discussPostService.updateScore(postId, score);
        // 将更新后的帖子同步到ES搜索服务器
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
