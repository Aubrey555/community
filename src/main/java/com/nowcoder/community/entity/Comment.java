package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@ToString
@Data
@AllArgsConstructor
public class Comment {//该实体类表示对于帖子的评论,以及对于帖子的评论的评论
    private Integer id;
    private Integer userId;
    private Integer entityType;//实体类型,表示评论的目标和内容(1:帖子 2:评论 3:用户 4:题目 5:课程视频),此项目中,一般都为1或者2,表示当前评论的对象为帖子或者其他评论
    private Integer entityId;//表示当前评论的目标是哪个帖子(映射帖子的id)
    private Integer targetId;//当对帖子的某个评论x进行评论时,使用此属性记录评论x的用户id
    private String content;//评论内容
    private Integer status;//0:当前帖子正常  1:帖子被删除
    private Date createTime;//评论时间
}
