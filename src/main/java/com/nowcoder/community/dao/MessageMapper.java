package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

//@Mapper
public interface MessageMapper {
    /**
     * 1.查询当前用户的所有会话列表,针对每个会话只返回一条最新的私信.(即每个会话只有一个最新私信返回到界面上显示)
     * @param userId    传入当前用户的id(可能是发送方,也可能是接受方)
     * @param offset    分页数据：当前页的起始行,即表示当前页的起始行应该为第几条数据
     * @param limit     每页多少条数据
     * @return
     */
    List<Message> selectConversations(int userId, int offset, int limit);

    //2.查询当前用户的会话数量(等价于1返回的list集合的大小)
    int selectConversationCount(int userId);

    //3.查询某个会话所包含的私信列表(通过conversationId对应的会话,返回两个用户之间所有的私信集合)
    List<Message> selectLetters(String conversationId, int offset, int limit);

    //4.查询某个会话所包含的私信数量.(也就是所有相同conversationId的总数)
    int selectLetterCount(String conversationId);

    //5.查询未读私信的数量
        // 将conversationId作为动态条件进行拼接,传入则表示查询当前用户userId和某个指定用户conversationId的所有未读私信数量
        // 不传入conversationId则表示查询当前用户userId和其他所有用户的未读私信数量
    int selectLetterUnreadCount(int userId, String conversationId);

    //6. 新增消息
    int insertMessage(Message message);

    //7. 修改消息的状态(根据多个私信的id进行修改,可以设置已读或者删除等)
    int updateStatus(List<Integer> ids, int status);

    //8. 查询某个主题下最新的通知(最新时间):传入指定用户指定主题
    Message selectLatestNotice(int userId, String topic);

    //9. 查询某个主题所包含的通知数量(即此主题下通知的总数)
    int selectNoticeCount(int userId, String topic);

    //10.查询未读的通知的数量(未读通知数量),此时允许topic=null,表示查询所有主题(点赞评论关注)的未读数量
    int selectNoticeUnreadCount(int userId, String topic);

    //11.查询某个主题所包含的通知列表
    List<Message> selectNotices(int userId, String topic, int offset, int limit);
}
