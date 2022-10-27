package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/20 17:43
 * @Description:
 */
@SpringBootTest
public class MapperTest {
    @Autowired
    UserMapper userMapper;

    @Autowired
    DiscussPostMapper discussPostMapper;//自动注入该类的接口
    //测试通过id/name/email查询user

    @Autowired
    LoginTicketMapper loginTicketMapper;//测试LoginTicketMapper接口
    @Test
    public void getUser(){
        User user = userMapper.selectById(1);//通过id查询user
        System.out.println(user);

        user = userMapper.selectByName("SYSTEM");//通过name查询User
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder1@sina.com");//通过email进行查询
        System.out.println(user);
    }
    //测试插入数据
    @Test
    public void insertUser(){
        User user = new User("root", "Aubrey00");
        int i = userMapper.insertUser(user);
        System.out.println(i);//返回受影响行数
    }
    //测试修改条件
    @Test
    public void testUpdate(){
        int i = userMapper.updateStatus(150, 1);//修改新加入用户的状态
        System.out.println(i);
    }
    //测试DiscussMapper接口的两个方法
        //通过用户id查询该用户的帖子总数(@Param注解中的属性表示为当前参数的userId的别名)
        //int selectDiscussPostRows(@Param("userId") int userId);
        //分页显示: 通过用户id返回该用户的帖子(个人主页的帖子)
        //List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);
    @Test
    public void testDiscussPost(){
        //表示查询userId=101用户的所有数据:并分页显示第1页的数据,每页10条
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(101, 0, 10);
        for (DiscussPost post:discussPosts){
            System.out.println(post);
        }
        //查询discuss_post帖子表中的所有未拉黑的帖子总数(userId=0表示查询所有表中所有帖子总数)
        int rows = discussPostMapper.selectDiscussPostRows(0);
        System.out.println(rows);
    }

    //测试loginTicketMapper接口的插入条件
    @Test
    public void testLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(10);
        loginTicket.setTicket("qwertyuiop");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000*60*10));//当前时间+10分钟
        int i = loginTicketMapper.insertLoginTicket(loginTicket);//测试插入条件
    }
    //测试查询和更新条件
    @Test
    public void testLoginTicket1(){
        LoginTicket ticket = loginTicketMapper.selectByTicket("qwertyuiop");
        System.out.println(ticket);
        int i = loginTicketMapper.updateStatus("qwertyuiop", 1);//更新用户凭证为1
    }
}
