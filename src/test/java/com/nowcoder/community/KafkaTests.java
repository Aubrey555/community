package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@SpringBootTest
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;//生产者组件,用于发送消息

    //生产者发布消息是主动的,发布消息到消息队列中,生产者接收消息队列中指定主题的消息有一点延迟
    //控制台自动输出消费者监听的指定主题的消息
    @Test
    public void testKafka() {
        //正式启动服务器时，在启动的过程中，topic会被自动创建。而测试代码里却不行，对于测试代码，你需要提前手动创建topic。
        //在前面cmd测试中,已经创建了test主题
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "在吗");
        try {
            Thread.sleep(1000 * 10);//生产者发布消息结束后主线程休息一会,
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@Component
class KafkaProducer {   //kafka的生产者(类似于服务器,发送系统消息)
    @Autowired
    private KafkaTemplate kafkaTemplate;//生产者依靠KafkaTemplate发送消息到主题
    /**
     * 生产者发送消息到指定主题
     * @param topic     主题
     * @param content   消息内容
     */
    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic, content);
    }
}

@Component
class KafkaConsumer {   //kafka的消费者(类似于客户端,接收系统消息)
    /**
     * 消费者使用@KafkaListener监听器,接收指定主题topics = {"test"}上发布的消息
     * @param record        Spring监听到消息后,整合后返回到ConsumerRecord此参数,可以对此参数进行处理
     */
    @KafkaListener(topics = {"test"})   //在测试中创建了test主题,因此消费者可以在此主题上接收数据
    public void handleMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }
}