package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;


@Data
@ToString
public class Event {//该类为kafka发送系统通知中触发事件对应的实体类,封装触发事件所有的信息

    private String topic;//每个触发事件对应的主题,即事件类型
    private int userId;//触发事件的用户(事件的来源)
    private int entityType;//触发事件对应的实体类型(即该事件是作用于帖子/评论/用户?)
    private int entityId;//实体类型的id,唯一标识
    private int entityUserId;//该实体对应的用户(即帖子/评论/用户 对应的人的id)
    private Map<String, Object> data = new HashMap<>();//处理触发事件产生的额外信息(不可预判,使用此data进行存储,比如某用户A对用户B关注/点赞的时间)

    public String getTopic() {
        return topic;
    }
    //此时即传入主题topic后可以一直.setXX进行赋值,比如event.setTopic(xxx).setUserId(xxx)...
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }
    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }
    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }
    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }
    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }
    public Event setData(String key, Object value) {//直接可以向data中传入key value数据
        this.data.put(key, value);
        return this;
    }
}
