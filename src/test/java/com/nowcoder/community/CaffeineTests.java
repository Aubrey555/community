package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CaffeineTests {

    @Autowired
    private DiscussPostService postService;

    //为了进行压力测试,向数据库中存储30w条数据
    @Test
    public void initDataForTest() {
        for (int i = 0; i < 300000; i++) {
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("互联网求职暖春计划");
            post.setContent("今年的就业形势，确实不容乐观。过了个年，仿佛跳水一般，整个讨论区哀鸿遍野！19届真的没人要了吗？！18届被优化真的没有出路了吗？！大家的“哀嚎”与“悲惨遭遇”牵动了每日潜伏于讨论区的牛客小哥哥小姐姐们的心，于是牛客决定：是时候为大家做点什么了！为了帮助大家度过“寒冬”，牛客网特别联合60+家企业，开启互联网求职暖春计划，面向18届&19届，拯救0 offer！");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            postService.addDiscussPost(post);
        }
    }

    //压力测试:使用本地缓存优化性能
    @Test
    public void testCache() {
        //0:所有帖子    0:表示第一页数据     10:每页显示10条数据      orderMode =1:按照热度进行显示(即帖子分数)。=0 :表示按照默认方式进行排序(时间排序,此时缓存中没有)
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第一次访问,本地缓存中没有,直接从数据库中获取,并初始化到本地缓存,性能很差
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第二次访问,从本地缓存中访问,性能好
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));//第三次访问,从本地缓存中访问,性能好
        System.out.println(postService.findDiscussPosts(0, 0, 10, 0));//对于orderMode = 0,按照时间进行排序的帖子,本地内容中没有缓存,性能差
    }

}
