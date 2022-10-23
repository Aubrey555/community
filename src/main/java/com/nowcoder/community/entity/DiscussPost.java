package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/19 17:15
 * @Description:    该实体类映射数据库的discuss_post帖子表
 *   `id` INT(11) NOT NULL AUTO_INCREMENT,
 *   `user_id` VARCHAR(45) DEFAULT NULL,
 *   `title` VARCHAR(100) DEFAULT NULL,
 *   `content` TEXT,
 *   `type` INT(11) DEFAULT NULL COMMENT '0-普通; 1-置顶;',
 *   `status` INT(11) DEFAULT NULL COMMENT '0-正常; 1-精华; 2-拉黑;',
 *   `create_time` TIMESTAMP NULL DEFAULT NULL,
 *   `comment_count` INT(11) DEFAULT NULL,
 *   `score` DOUBLE DEFAULT NULL,	-- 帖子的分数(按照热度进行排名)
 */
@Data   //自动生成get set方法
@AllArgsConstructor //生成全参构造器
@NoArgsConstructor  //生成无参构造器
@ToString   //生成toString方法
public class DiscussPost {
    private int id;
    private int userId;
    private String title;//帖子标题
    private String content;//帖子内容
    private int type;   //帖子类型:'0-普通; 1-置顶;',
    private int status; //帖子状态:'0-正常; 1-精华; 2-拉黑;',
    private Date createTime;//帖子创建时间
    private int commentCount;//帖子的评论数量(每一个帖子对应的评论都在一个表comment中)
    private double score;//当前帖子的分数(之后用)
}
