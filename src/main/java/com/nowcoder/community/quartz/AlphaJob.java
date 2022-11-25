package com.nowcoder.community.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

//该类表示SpringQuartz组件中的定时任务逻辑(实现job接口给出) :测试的定时任务
public class AlphaJob implements Job {//该类用于首次配置Quartz参数到数据库中
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println(Thread.currentThread().getName() + ": execute a quartz job.");
    }
}
