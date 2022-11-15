package com.nowcoder.community.dao;

import com.nowcoder.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/19 17:18
 * @Description:
 */
@Mapper //该注解用于标识此类为一个mapper接口,用于操作数据库(或者在主配置类上加@MapperScan注解，则其他的mapper就可以不用标注@Mapper注解)
public interface UserMapper {
    //该方法标识根据id查询用户
    @Select("select * from user where id = #{id}")
    User selectById(int id);

    User selectByName(String username);//根据用户名查询用户

    User selectByEmail(String email);//根据邮箱查询id

    int insertUser(User user);//加入用户

    int updateStatus(int id, int status);//通过id修改用户状态

    int updateHeader(int id, String headerUrl);//通过用户id,更新用户头像(url路径)

    int updatePassword(int id, String password);//根据用户id更新密码

}
