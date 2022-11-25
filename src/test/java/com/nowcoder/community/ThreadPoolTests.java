package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {  //该测试类用于演示线程池功能,JDK/Spring/Quartz等线程池

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);//日志输出内容

    // JDK普通线程池:ExecutorService,初始化线程池中包含5个线程
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    // JDK可执行定时任务的线程池:ScheduledExecutorService,初始化线程池中也包含5个线程,可执行定时任务
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // Spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    // Spring可执行定时任务的线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired  //Spring线程池简化使用时线程体所在业务组件
    private AlphaService alphaService;

    /**
     * 在Junit测试中,测试线程使用,只要测试方法一结束(后面没有任务逻辑),则测试直接结束,不论此时有没有子线程正在执行
     *      因此此处定义sleep方法,在test测试方法逻辑结束后,手动阻塞一会,再停止test(),即test()停止后,开启的子线程(JDK/Spring线程)也会结束
     * @param m
     */
    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 1.JDK普通线程池
    @Test
    public void testExecutorService() {
        //1.run()中定义开启的线程需要执行的任务逻辑task
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("JDK普通线程池测试任务...");
            }
        };
        //2.执行10次任务task,使用线程池executorService进行处理
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);//反复使用executorService线程池中的5个线程
        }
        //3.阻塞10s后test()方法结束,则开启的executorService线程池中的子线程也会关闭
        sleep(10000);
    }

    // 2.JDK定时任务线程池
    @Test
    public void testScheduledExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("JDK定时任务线程池测试...");
            }
        };
        //以固定的时间执行任务:
            //task->执行的任务,10000->延迟时间10s后再进行执行,而不是立即执行,1000->时间间隔1s(反复执行),TimeUnit.MILLISECONDS->时间单位为ms
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);

        //阻塞30s后停止线程
        sleep(30000);
    }

    // 3.测试Spring普通线程池:ThreadPoolTaskExecutor:此线程池可以配置线程池的一些参数,比JDK的线程池更加灵活,优先使用
    @Test
    public void testThreadPoolTaskExecutor() {
        //1.run()中定义开启的线程需要执行的任务逻辑task
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Spring普通线程池测试任务...");
            }
        };
        //2.执行10次任务task,使用线程池executorService进行处理
        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }
        //3.阻塞10s后test()方法结束,则开启的executorService线程池中的子线程也会关闭
        sleep(10000);
    }

    // 4.Spring定时任务线程池:ThreadPoolTaskScheduler
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Spring定时任务线程池ThreadPoolTaskScheduler测试...");
            }
        };
        //任务开始执行时间:当前时间+延迟10s
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        //执行任务: 1000时间间隔=1s
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);
        //阻塞30s,即30s后test()线程停止,Spring线程结束
        sleep(30000);
    }

    // 5.Spring普通线程池(简化)
    @Test
    public void testThreadPoolTaskExecutorSimple() {
        for (int i = 0; i < 10; i++) {
            //execute1()方法上调用了@Async注解,则会自动使用Spring普通线程池进行调用执行,不再需要线程池去调用执行,方法自身即为一个线程体
            alphaService.execute1();//线程体被执行
        }
        sleep(10000);
    }

    // 6.Spring定时任务线程池(简化)
    @Test
    public void testThreadPoolTaskSchedulerSimple() {
        //程序启动时自动调用execute2()方法,启动Spring定时任务逻辑(该方法内给定了定时执行所需参数)
        sleep(30000);
    }

}
