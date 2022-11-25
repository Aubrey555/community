package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
//@Scope("prototype")
public class AlphaService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;



    /**
     * 通过声明式事务实现功能:先增加用户到数据库,再增加一个自动发布的帖子到数据库。(需要同时成功,保证业务完整性)
     *      isolation属性:设置隔离级别(读已提交,可处理不可重复读和幻读)
     *      propagation属性:设置事务的传播机制(即当前业务方法A可能会存在业务方法B,两个方法都被事务进行管理,事务传播机制解决此种事务交叉问题)
     *             REQUIRED: 表示支持当前事务(外部事务),如果外部事务不存在则创建新事务.(A调用B,对于事务B来说,A就是外部事务,即当前事务)
     *             REQUIRES_NEW: 创建一个新事务,并且暂停当前事务(外部事务).(A调用B,如果B存在事务,则暂停掉)
     *             NESTED: 如果当前存在事务(外部事务),则嵌套在该事务中执行(独立的提交和回滚),否则就会REQUIRED一样.
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        //1.新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //2.新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("Hello");
        post.setContent("新人报道!");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);
        //3.此处报错,因此上面两个新增业务不会执行,会进行回滚
        Integer.valueOf("abc");
        //4.如果执行成功,则事务提交,此处会返回"ok"
        return "ok";
    }

    @Autowired
    private TransactionTemplate transactionTemplate;
    /**
     * 演示编程式事务使用:需要注入TransactionTemplate组件
     * @return
     */
    public Object save2() {
        //1.声明隔离级别
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        //2.声明传播机制
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        //3.编程式事务使用,调用execute()方法,传入TransactionCallback回调接口(使用匿名内部类实现)
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override   //该方法返回的类型和接口类型一致
            public Object doInTransaction(TransactionStatus status) {
                //1.新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                //2.新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("你好");
                post.setContent("我是新人!");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);
                //3.报错,事务进行回滚,不会插入到数据库
                Integer.valueOf("abc");
                //4.如果事务提交,返回Ok
                return "ok";
            }
        });
    }

    // 让该方法在多线程环境下,被异步的调用(与主线程异步).
    //@Async  //该注解将此方法作为一个线程体被执行,使用Spring的普通线程池
    public void execute1() {
        logger.debug("execute1");
    }

    //@Scheduled注解可以将该方法作为一个定时任务的线程体,被定时的执行(initialDelay = 10000延迟10s后执行,fixedRate = 1000定时周期1s间隔执行)
        //启动时被自动调用,不需要主动调
    //@Scheduled(initialDelay = 10000, fixedRate = 1000)
    public void execute2() {
        logger.debug("execute2");
    }

}
