package com.nowcoder.community.util;

//该工具类用于生成Redis数据库中的key(通过静态方法即可访问)
public class RedisKeyUtil {

    private static final String SPLIT = ":";//用来拼接Redis中key的常量
    //此时需要存储帖子/评论等相关实体的赞
    private static final String PREFIX_ENTITY_LIKE = "like:entity";//对于帖子/评论等相关实体赞的前缀常量(后面再加上相关id)
    private static final String PREFIX_USER_LIKE = "like:user";//以User用户为key的前缀常量(每个帖子/评论等对应的用户)
    private static final String PREFIX_FOLLOWEE = "followee";//A关注了B,则B是A的目标followee,因此根据该前缀对应的键,可以得到A关注用户总数
    private static final String PREFIX_FOLLOWER = "follower";//A关注了B,则A是B的粉丝follower,因此根据该前缀对应的键可以得到B的所有粉丝总数
    private static final String PREFIX_KAPTCHA = "kaptcha";//验证码键key对应的前缀
    private static final String PREFIX_TICKET = "ticket";//登陆凭证key对应的前缀
    private static final String PREFIX_USER = "user";//用户key对应的前缀
    private static final String PREFIX_UV = "uv";   //uv表示独立访客key对应的前缀
    private static final String PREFIX_DAU = "dau"; //dau表示日活跃用户key对应的前缀
    private static final String PREFIX_POST = "post";//定时任务统计中所需的帖子前缀


    /**
     * 生成某个实体(帖子/评论等相关实体,以及该实体对应的id)的key值
     *      like:entity:entityType:entityId -> set(userId)
     *      即哪个用户向该实体点赞,则将该用户id加到实体集合中
     * @param entityType
     * @param entityId
     * @return
     */
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 得到某个用户对应的键
     * @param userId    例如:like:user:userId -> int
     * @return
     */
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /**
     * 获取某个用户user关注的实体对象 对应的键
     *      键key:                                                       followee:userId:entityType ->
     *      值value: 对应集合ZSet(存储关注的实体对象的id,以当前时间作为分数进行排序): zset(entityId,now)
     * @param userId    用户user的userId
     * @param entityType    关注的实体对象
     * @return
     */
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /**
     * 获取某个实体(entityType)所拥有的粉丝集合Set
     *      键key:follower:entityType:entityId   ->
     *      值value: 对应集合ZSet(存储当前实体的粉丝id,以及当前时间作为有序集合sortSet排序分数)  zset(userId,now)
     * @param entityType
     * @param entityId
     * @return
     */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 生成验证码对应的key
     * @param owner     准备登陆的用户随机生成的一个字符串owner,作为临时凭证
     * @return
     */
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /**
     * 获取用户登录凭证对应的key
     * @param ticket    传入登陆成功的凭证ticket字符串
     * @return
     */
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /**
     * 获取用户对应的key
     * @param userId    用户的id
     * @return
     */
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    /**
     * 得到单日UV对应的key,即每天都统计网站的独立访客(包含非注册的匿名用户,根据ip地址进行统计)
     * @param date  传入某个日期的字符串(年月日)
     * @return
     */
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /**
     * 得到一个区间的UV对应的key(比如一周七天的UV)
     * @param startDate     开始日期
     * @param endDate       结束日期
     * @return
     */
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * 得到某个日期的日活用户对应的key(只对登录用户按照userId进行统计)
     * @param date  单日对应日期
     * @return
     */
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /**
     * 得到某个区间的活跃用户对应的key
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * 得到统计帖子分数对应的key
     *      redis中存储的是产生变化的多个帖子,不是某一个,因此不用传入帖子id
     * @return
     */
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }

}
