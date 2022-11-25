package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component  //该类表示消费者组件,使用Spring进行管理,用于接收指定主题中的消息(事件Event)
@Slf4j      //处理事件中的问题使用日志记录
public class EventConsumer implements CommunityConstant {
    //消费者接收消息队列中指定主题的消息(此时即封装为事件Event对象),对消息再进行处理即可(即将Event消息封装为Message对象,使用私信业务逻辑MessageService进行处理)
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;//服务器中图片存放的位置(配置文件中获取)
    @Value("${wk.image.command}")
    private String wkImageCommand;//服务器中图片转化命令所在地址
    @Value("${qiniu.key.access}")
    private String accessKey;//AK密钥,确认是否为对象空间的创建者

    @Value("${qiniu.key.secret}")
    private String secretKey;//文件加密的密钥

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;//存储网页生成的图片文件的对象空间

    @Value("${qiniu.bucket.share.url}")     //此处用不到,可以取消
    private String shareBucketUrl;//存储网页生成的图片文件的对象空间的域名

    @Autowired      //Spring执行定时任务的线程池:用于开启线程将网页模板生成的图片文件保存到七牛云服务器
    private ThreadPoolTaskScheduler taskScheduler;

    /**
     * 此时使用handleCommentMessage()方法同时接收三个主题的通知(评论/点赞/关注);一个主题也可以同时被多个消费者接收 (多对多关系)
     *      并且此时静态页面中接收到的三个主题的通知形式十分接近,因此可以将三个主题的通知在一个业务方法内进行处理。
     * @param record    Spring将在指定主题监听到的对象信息包装为ConsumerRecord对象返回到此方法中作为参数,用户可以对此参数record进行处理,即处理主题的通知Event
     */
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})  //使用该注解监听消息队列中指定主题的消息(Event对象)
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");//发送了空消息
            return;
        }

        //1.接收到的消息为JSON格式的字符串(生产者在发送通知中设置)并且封装在record中,此时将此record对象(JSON格式的字符串)解析为Event对象类型
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }

        //2.发送站内通知(即发送系统通知,将Message对象信息插入到数据库的message表中)
            //消费者处理接收到的Event对象的方式:此时处理方式与用户之间发送私信的处理逻辑一致,将得到的事件Event对象中的数据取出,
            //封装为私信Messgae对象,使用MessageService私信服务添加到message进行处理,处理逻辑即与用户之间发送/接收私信的业务一致,区别在于此时message对象存储内容发生变化)。
        Message message = new Message();    //构造Messgae对象
        message.setFromId(SYSTEM_USER_ID);  //发送私信方为系统用户(1)
        message.setToId(event.getEntityUserId());   //接收信息方为 event事件对象的entityUserId
        message.setConversationId(event.getTopic()); //Messgae的会话id  设置为  当前事件对应的主题
        message.setCreateTime(new Date());  //当前时间

            //构造Message对象的content属性,使用map存储数据,最后转化为JSON格式的字符串即可
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());   //触发事件的来源(userId,即触发事件的用户)
        content.put("entityType", event.getEntityType());   //触发事件对应的实体类型(即该事件是作用于帖子/评论/用户?)
        content.put("entityId", event.getEntityId());//实体类型的id,唯一标识

            //处理触发事件产生的额外信息data属性(即Event对象的data属性中可能存放用户A对用户B 点赞/评论/关注 的时间等),取出data中的所有信息,存储到content中
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
            //将Map属性的content转化为JSON格式的字符串,存入Message对象中
        message.setContent(JSONObject.toJSONString(content));
            //此时将生产者加入到消息队列中的Event事件对象,封装为Message对象后,并使用addMessage()存储到数据库的message表中,在控制层进行处理。
        messageService.addMessage(message);
    }


    /**
     * 该组件用于处理发帖事件:监听器监听到TOPIC_PUBLISH主题的消息队列中一有数据,则调用elasticsearchService类的方法,将该帖子加入到ES服务器
     * @param record    消费者组件将监听到的消息转化为形参record对象
     */
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }
        //1.接收到的消息为JSON格式的字符串(生产者在发送通知中设置)并且封装在record中,此时将此record对象(JSON格式的字符串)解析为Event对象类型
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }
        //从事件消息event中通过帖子id得到帖子对象Post,将帖子存储到ES服务器中
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }
    /**
     * 该组件用于处理删帖事件:监听器监听到TOPIC_DELETE主题的消息队列中一有数据,则调用elasticsearchService类的方法,将该帖子从ES服务器中删除
     * @param record    消费者组件将监听到的消息转化为形参record对象
     */
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }
        //1.接收到的消息为JSON格式的字符串(生产者在发送通知中设置)并且封装在record中,此时将此record对象(JSON格式的字符串)解析为Event对象类型
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }
        //从事件消息event中得到帖子id从ES服务器中进行删除
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    /**
     * 消费者处理TOPIC_SHARE事件:即处理网页生成图片分享
     * @param record    消费者组件将监听到的消息转化为形参record对象(监听到的消息即为封装的Event对象)
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        //1.对监听到的参数进行判断
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }
        //2.得到监听到的图片分享事件Event中的信息
        String htmlUrl = (String) event.getData().get("htmlUrl");//需要分享为图片的网页地址
        String fileName = (String) event.getData().get("fileName");//生成的图片的文件名
        String suffix = (String) event.getData().get("suffix");//生成图片保存的后缀
        //3.通过Runtime.getRuntime().exec()执行网页生成图片的cmd命令,生成长图
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;//图片存储到本地服务器的位置wkImageStorage
        try {
            Runtime.getRuntime().exec(cmd);//执行生成图片的命令,保存到本地服务器wkImageStorage中
            log.info("生成长图成功: " + cmd);//生成图片的提示信息
        } catch (IOException e) {
            log.error("生成长图失败: " + e.getMessage());
        }

        //启用定时器,监视该图片,一旦生成了,则上传至七牛云.(因为图片生成命令需要一段时间加载,只有生成图片完成了才进行上传;提示信息会直接输出后,到此处,因此需要开启定时任务进行监控)
            //此时在分布式环境下,不存在多个线程同时处理的清空。因为消费者consumer具有排斥机制,一个消费者处理此消息队列的事件时,其他消费者则不会执行此定时任务
            //也就是哪个服务器上的消费者组件首先抢占到此消息队列share的事件,才开启此定时任务,其他服务器的消费者组件则不在对此事件进行处理,因此不会开启定时任务,则不会存在同时执行
        UploadTask task = new UploadTask(fileName, suffix);
            //scheduleAtFixedRate()方法的返回值:封装定时器的任务执行状态,可以用来停止定时器
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);//触发定时器:每隔500ms(0.5s)执行一次此定时任务task。
        task.setFuture(future);//上传图片的定时任务通过返回值future来进行停止
    }

    //上传图片文件的定时任务,开启线程进行处理
    class UploadTask implements Runnable {
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;

        // 启动的定时任务的返回值,可以用来停止此定时器任务
        private Future future;
        // 定时任务的开始时间
        private long startTime;
        // 上传图片的次数(可能会多次上传才能成功)
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();//设置开始时间为当前时间
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 1.生成图片失败,停止定时任务
            if (System.currentTimeMillis() - startTime > 30000) {//此时即上传图片的时间超过30s,则停止此任务
                log.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true);//停止当前定时任务
                return;//直接退出
            }
            // 2.上传图片失败,也停止定时任务
            if (uploadTimes >= 3) {//上传了3次还未成功,停止
                log.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }

            // 3.得到本地服务器存储的网页转化的图片文件file
            String path = wkImageStorage + "/" + fileName + suffix;//存储的图片路径
            File file = new File(path);
            // 4.只要插件转化的图片文件在本地服务器存在,就进行上传
            if (file.exists()) {
                // 4.1 日志记录
                log.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 4.2 设置响应信息:即将资源上传给七牛云后,规定七牛云可以做出的响应(在UserController中上传头像文件也进行了设置)
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));//响应信息内容
                // 4.3 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                    // 利用auth生成上传凭证字符串(传入对象空间名成/生成的文件名/过期时间/响应信息)
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);//上传凭证的字符串
                // 4.4 指定上传机房(浏览器上传图像文件使用js异步请求指定上传地址)
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));//华北地区的服务器
                try {
                    // 4.5 开始上传图片:manager.put(文件路径,文件名,上传凭证,一般为null,文件类型,一般为false)方法用于上传图片,七牛云的sdk提供此方法
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 4.6 处理响应结果:返回JSON格式的字符串给客户端
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        log.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        log.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);//上传成功,停止定时任务
                    }
                } catch (QiniuException e) {
                    log.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {//此时图片还未生成
                log.info("等待图片生成[" + fileName + "].");
            }
        }
    }

}
