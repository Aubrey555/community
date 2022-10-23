package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/19 17:10
 * @Description: 该实体类映射community数据库的user表
 *   `id` INT(11) NOT NULL AUTO_INCREMENT,
 *   `username` VARCHAR(50) DEFAULT NULL,
 *   `password` VARCHAR(50) DEFAULT NULL,
 *   `salt` VARCHAR(50) DEFAULT NULL,
 *   `email` VARCHAR(100) DEFAULT NULL,
 *   `type` INT(11) DEFAULT NULL COMMENT '0-普通用户; 1-超级管理员; 2-版主;',
 *   `status` INT(11) DEFAULT NULL COMMENT '0-未激活; 1-已激活;',
 *   `activation_code` VARCHAR(100) DEFAULT NULL,
 *   `header_url` VARCHAR(200) DEFAULT NULL,
 *   `create_time` TIMESTAMP NULL DEFAULT NULL,
 */
@Data   //自动生成get set方法
@AllArgsConstructor //生成全参构造器
@NoArgsConstructor  //生成无参构造器
@ToString   //生成toString方法
public class User {
    private int id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createTime;
    //通过username和pwd创建用户
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
