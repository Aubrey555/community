package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service    //service层,此时只是处理简单逻辑,但后面可能会进行过滤等操作
@Slf4j
public class DiscussPostService {
    @Resource   //注入DiscussPostMapper接口
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;//注入组件


    @Value("${caffeine.posts.max-size}")
    private int maxSize;//本地缓存存储的数据的最大数量
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;//缓存数据的过期时间

    // 本地缓存Caffeine核心接口: Cache, 常用子接口: LoadingCache(同步缓存,最常用,此处使用), AsyncLoadingCache

    // 帖子列表缓存(按照key缓存value,value为一个集合,存储缓存的热帖;当前业务下key即为键,string类型,内容为当前页的页码和每页显示条数,在初始化方法中给出)
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存(value为一个整数,缓存帖子总数,key为整数,当前应用下为用户id)
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct  //由该注解修饰的方法,会在构造器初始化结束后执行,此处即在SpringBoot项目启动后执行
    public void init() {
        // 初始化帖子列表缓存(固定格式)
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)   //缓存最大数据量
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)  //缓存过期时间,秒s
                .build(new CacheLoader<String, List<DiscussPost>>() {   //调用builder()方法,返回LoadingCache对象
                    @Nullable
                    @Override   //该方法load()实现查询数据库,得到本地缓存中初始化数据的方法(即如何查询数据库数据到本地缓存)
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        //此处的key即为该热帖缓存postListCache的键,为String类型,组成内容为: offset + ":" + limit,当前页的页码+":"+每页显示数据
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");//没有传入正确的键key
                        }

                        String[] params = key.split(":");//利用:进行分隔,param[0]即为当前页的页码offset;param[1]中存储每页显示数据
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");//一定为两个值
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 此处可以更新二级缓存: Redis -> mysql
                            //先访问二级缓存Redis,如果Redis中没有再访问mysql

                        log.debug("load post list from DB.");
                        //即当前一级缓存只适用于userId=0即所有帖子数据,orderMode=1即按照热帖进行排序
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });

        // 初始化帖子总数缓存(固定格式)
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        //此处的Integer key即为本地缓存postRowsCache的键,查询给定key(即某个用户)的帖子总数
                            //如果key为0,即表示所有的帖子总数

                        //此处也可先查询二级缓存    ->    mysql数据库

                        log.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    /**缓存优化:先使用一级缓存(本地缓存)查询当前热门帖子,再查询数据库mysql(二级缓存redis自己完善)
     *      分页方式返回当前用户的所有帖子
     * @param userId    用户id,如果为0则表示查询所有用户的所有帖子,也就是访问首页
     * @param offset    分页所需数据:当前页的页码
     * @param limit     分页所需数据:每页显示条数
     * @param orderMode    帖子排序模式:默认值为0,表示按照时间进行排序(如果传入为1,则按照热度进行排序)
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit,int orderMode) {
        if (userId == 0 && orderMode == 1) {//因此只有访问首页所有的热门帖子,才启用一级缓存进行查询
            //传入数据为本地缓存对应的键String
            return postListCache.get(offset + ":" + limit);
        }
        //否则,对于其他页面的帖子,或者按照时间更新的帖子仍查询数据库访问
        log.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }
    /**缓存优化:先使用一级缓存(本地缓存)查询当前帖子总数,再查询数据库mysql(二级缓存redis自己完善)
     *      通过用户id查询该用户的帖子总数(如果为0表示所有用户,即所有帖子总数)
     * @param userId        用户的id
     * @return
     */
    public int findDiscussPostRows(int userId) {
        if (userId == 0) {//如果查询所有帖子总数,则通过一级缓存进行查询
            return postRowsCache.get(userId);
        }
        //否则,及查询某个用户的帖子总数,则查询数据库数据
        log.debug("load post rows from DB.");
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

    /**
     * 根据帖子id更新当前帖子类型
     * @param id
     * @param type  普通/置顶
     * @return
     */
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

}
