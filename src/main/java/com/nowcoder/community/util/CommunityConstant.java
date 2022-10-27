package com.nowcoder.community.util;

/**
 * 该接口用于定义某个用户的激活状态,在进行邮件激活请求时使用
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

}
