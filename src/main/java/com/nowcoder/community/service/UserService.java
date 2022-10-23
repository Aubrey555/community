package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    /**
     * 该方法标识根据用户id查询用户
     * @param id
     * @return
     */
    public User findUserById(int id) {
        return userMapper.selectById(id);
    }
}
