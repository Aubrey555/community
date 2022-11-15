package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component  //该类表示生产者组件,发送指定消息(此时消息即被包装为事件对象Event)到指定主题
public class EventProducer {
    @Autowired  //注入组件,用于发送消息
    private KafkaTemplate kafkaTemplate;

    /**
     * 处理事件的方法(即生产者发送消息到消息队列)
     * @param event
     */
    public void fireEvent(Event event) {
        // 将事件发布到指定的主题(将Event事件对象封装为JSON格式的字符串(需要在前端页面进行处理,展示到浏览器))
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
