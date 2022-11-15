package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
}
