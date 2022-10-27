package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@AllArgsConstructor //生成全参构造器
@NoArgsConstructor  //生成无参构造器
@ToString   //生成toString方法
public class LoginTicket {
    private int id; //主键
    private int userId;//用户的唯一id
    private String ticket;//凭证:随机字符串,即对应该用户的唯一标识
    private int status;//用户状态(0--正常有效,1--过期无效)
    private Date expired;////日期(过期时间)
}
