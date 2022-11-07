package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration  //声明该类为一个配置类
public class RedisConfig {
    //可以通过redistemplate访问redis
    @Bean   //重新配置redis中的组件 RedisTemplate,使得键值对为<String,Object>,SpringBoot整合为<Object,Object>
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        //1.使用template访问数据库,则需要将数据库连接工厂进行注入,实例化RedisTemplate
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        //2.配置template中设置key的序列化方式(即数据转换方式),此方式即返回能序列化字符串的一个序列化器,从而redis的key值为String
        template.setKeySerializer(RedisSerializer.string());
        //3.设置普通的value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //4.设置hash数据类型的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //5.设置hash数据类型的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        //6.调用该方法,使得对templates的作用生效。
        template.afterPropertiesSet();
        //7.返回templates模板
        return template;
    }
}
