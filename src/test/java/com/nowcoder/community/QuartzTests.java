package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class QuartzTests {
    //该测试类通过Quartz组件中的调度器Scheduler删除给定的定时任务,并且对数据库表中的定时任务信息进行清除(否则每次启动项目都会对该定时任务进行执行)
    @Autowired  //执行删除定时任务的调度器
    private Scheduler scheduler;

    @Test
    public void testDeleteJob() {
        try {
            //删除给定 定时任务"alphaJob"(以及所在组名)。并会清空数据库
            boolean result = scheduler.deleteJob(new JobKey("alphaJob", "alphaJobGroup"));
            System.out.println(result);//返回布尔值,表示成功或失败
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
