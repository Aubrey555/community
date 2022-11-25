package com.nowcoder.community.config;

import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 首次执行配置Quatrz参数 -> 同步数据到数据库 -> 以后Quatrz每次都访问数据库调用数据
@Configuration
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.Spring通过FactoryBean封装Bean的实例化过程.
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该Bean得到的是FactoryBean所管理的对象实例.

    // 配置JobDetail接口相关参数:对Job接口中的定时任务给出一些基本参数配置,比如任务的描述
     //@Bean
    public JobDetailFactoryBean alphaJobDetail() {
         //1.实例化对象
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        //2.声明factoryBean的相关参数
        factoryBean.setJobClass(AlphaJob.class);//管理的定时任务对象
        factoryBean.setName("alphaJob");//任务名称
        factoryBean.setGroup("alphaJobGroup");//任务所在组,多个任务可以处于同一组
        factoryBean.setDurability(true);//声明任务为持久保存(即使触发器trigger删除,此任务依然保存)
        factoryBean.setRequestsRecovery(true);//任务是可恢复的
        return factoryBean;//返回
    }

    /**
     * 配置Trigger接口参数(SimpleTriggerFactoryBean(简单模式) 或者 CronTriggerFactoryBean(复杂模式) )
     *      为Job接口定义的定时任务给出定时任务的时长,延迟时间,定时任务的优先级,开始时间,结束时间等参数。
     * @param alphaJobDetail    传入的参数alphaJobDetail即为JobDetailFactoryBean alphaJobDetail()组件所返回的factorybean对象,与方法同名
     * @return
     */
     //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
         //1.实例化Trigger接口对象的工厂bean
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        //2.该trigger接口的工厂bean的JobDetail接口为 alphaJobDetail(方法名)
        factoryBean.setJobDetail(alphaJobDetail);//此jobdetail接口对象与方法同名
        factoryBean.setName("alphaTrigger");//trigger名称
        factoryBean.setGroup("alphaTriggerGroup");//trigger所在组的组名
        factoryBean.setRepeatInterval(3000);//定时任务的频率(多长时间执行定时任务)
        factoryBean.setJobDataMap(new JobDataMap());//默认的job状态
        return factoryBean;
    }

    //具体配置内容可以看前面的测试配置类
    //配置JobDetail接口相关参数:对Job接口中的定时任务给出一些基本参数配置,比如任务的描述:
        // 执行定时刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    /**
     * 配置Trigger接口参数(SimpleTriggerFactoryBean(简单模式) 或者 CronTriggerFactoryBean(复杂模式) )
     *      为Job接口定义的定时任务给出定时任务的时长,延迟时间,定时任务的优先级,开始时间,结束时间等参数。
     * @param postScoreRefreshJobDetail 传入的参数postScoreRefreshJobDetail即为上述方法组件所返回的factorybean对象,与方法同名
     * @return
     */
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);//5min刷新执行一次
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
}
