package com.nowcoder.community.util;

/**
 * 该接口用于定义项目中的一些常量
 */
public interface CommunityConstant {

    /**
     * 激活成功
     */
    int ACTIVATION_SUCCESS = 0;

    /**
     * 重复激活(重复点击激活邮件中的链接)
     */
    int ACTIVATION_REPEAT = 1;

    /**
     * 激活失败(激活码可能是编造的,数据库中不存在对应该激活码的邮件)
     */
    int ACTIVATION_FAILURE = 2;

    /**
     * 默认状态的登录凭证的超时时间(登陆时 没有勾上记住我)
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;//12h

    /**
     * 记住状态的登录凭证超时时间(登陆时 勾上了记住我)
     */
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;//记住密码100天

    /**
     * 实体类型: 表示帖子
     */
    int ENTITY_TYPE_POST = 1;

    /**
     * 实体类型: 表示评论
     */
    int ENTITY_TYPE_COMMENT = 2;
    /**
     * 实体类型:表示用户
     */
    int ENTITY_TYPE_USER = 3;

    /**
     * 主题: 评论
     */
    String TOPIC_COMMENT = "comment";

    /**
     * 主题: 点赞
     */
    String TOPIC_LIKE = "like";

    /**
     * 主题: 关注
     */
    String TOPIC_FOLLOW = "follow";

    /**
     * 主题:发帖
     */
    String TOPIC_PUBLISH ="publish";

    /**
     * 主题:删帖
     */
    String TOPIC_DELETE ="delete";

    /**
     * 主题:分享
     */
    String TOPIC_SHARE ="share";

    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID = 1;

    /**
     * 权限:普通用户(type=0)
     */
    String AUTHORITY_USER = "user";

    /**
     * 权限:管理员(type=1)
     */
    String AUTHORITY_ADMIN = "admin";

    /**
     * 权限:帖子版主
     */
    String AUTHORITY_MODERATOR = "moderator";
}
