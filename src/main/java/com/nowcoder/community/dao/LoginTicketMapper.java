package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/26 14:47
 * @Description: 登录凭证实体类的mapper接口,完成crud方法
 */
//@Mapper       //已经在主类进行所有mapper接口的扫描,此处可以不用写该注解
public interface LoginTicketMapper {
    //对于crud方法,可以直接写在方法上

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })  //主键id自动生成,不用插入(可以通过多个字符串拼接)
    @Options(useGeneratedKeys = true, keyProperty = "id")   //id为主键,设置为自动生成,注入给id属性
    int insertLoginTicket(LoginTicket loginTicket);//插入一个凭证,表示一个用户登录

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })  //查询到的数据自动封装给LoginTicket(驼峰原则)
    LoginTicket selectByTicket(String ticket);//通过随机字符串ticket返回该登录凭证LoginTicket

    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if>",
            "</script>"
    })  //此处支持动态sql,在</script>标签内完成 (或者直接使用 update login_ticket set status=#{status} where ticket=#{ticket} )
    int updateStatus(String ticket, int status);//退出功能使用:修改此ticket对应凭证的状态,表示用户退出,而不是直接删除数据
}
