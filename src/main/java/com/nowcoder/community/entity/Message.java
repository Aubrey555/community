package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Message {  //该实体类表示用户的私信列表
    private int id; //唯一主键
    private int fromId;//消息的发送方(例如用户id: 111 112 113 114...),如果fromId=1,则表示为发送消息方为系统用户
    private int toId;//消息的接受方(例如用户id: 111 112 113 114...)
    //表示会话的一个字段(111_112之间的一个会话,112_113之间的一个会话,将小的用户id放在前面,表示两用户之间的一个会话。冗余数据,可以通过fromId等得到,但查询方便)
        //如果为系统通知,则fromId为1,接收用户不变,此时conversationId = comment/like/follow(表示一条评论通知/点赞通知/关注通知)
    private String conversationId;//会话id
    private String content;//消息内容
    private int status;//0表示私信未读,1表示已读,2表示删除该私信
    private Date createTime;
}
