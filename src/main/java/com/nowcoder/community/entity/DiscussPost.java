package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
//indexName指定ES中的索引名;当前ES版本为6.4.3,固定type=_doc,之后的版本会完全移除;shards表示分片,replicas表示副本;分片和副本任意配,一般都是根据服务器的处理能力进行配置
@Document(indexName = "discusspost",type="_doc",shards = 6,replicas = 3)    //该注解用于指定ES搜索引擎的索引和数据库中表的对应关系
@Data   //自动生成get set方法
@AllArgsConstructor //生成全参构造器
@NoArgsConstructor  //生成无参构造器
@ToString   //生成toString方法
public class DiscussPost {
    @Id     //该注解用于将此属性与ES搜索引擎中索引的id字段进行映射(索引的id字段,即为数据库中表的主键)
    private int id;
    @Field(type = FieldType.Integer)    //该注解表示普通字段,映射ES搜索引擎中的整数类型
    private int userId;

    //title和content即为需要使用搜索引擎搜索的字段内容(也就是根据帖子标题和内容搜索需要信息)
        //type = FieldType.Text表示此字段为索引中的文本类型,analyzer设置数据存储时的分词器("id_max_word"会将存储到ES的数据拆分为最多的单词,之后进行更大范围的筛选);
        // searchAnalyzer设置搜索时的分词器,"ik_smart"会将搜索数据进行智能匹配
    @Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String title;//帖子标题
    @Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String content;//帖子内容

    @Field(type = FieldType.Integer)    //该注解表示普通字段,映射ES搜索引擎中的整数类型
    private int type;   //帖子类型:'0-普通; 1-置顶;',
    @Field(type = FieldType.Integer)    //该注解表示普通字段,映射ES搜索引擎中的整数类型
    private int status; //帖子状态:'0-正常; 1-精华; 2-拉黑;',
    @Field(type = FieldType.Date)   //ES中映射为日期类型
    private Date createTime;//帖子创建时间
    @Field(type = FieldType.Integer)    //该注解表示普通字段,映射ES搜索引擎中的整数类型
    private int commentCount;//帖子的评论数量(每一个帖子对应的评论都在一个表comment中)
    @Field(type = FieldType.Double)    //该注解表示普通字段,映射ES搜索引擎中的整数类型
    private double score;//当前帖子的分数(之后用)
}
