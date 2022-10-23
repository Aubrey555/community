package com.nowcoder.community;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommunityApplicationTests {
	@Autowired
	UserMapper userMapper;
	//测试通过id/name/email查询user
	@Test
	public void getUser(){
		User user = userMapper.selectById(1);//通过id查询user
		System.out.println(user);

		user = userMapper.selectByName("SYSTEM");//通过name查询User
		System.out.println(user);

		user = userMapper.selectByEmail("nowcoder1@sina.com");//通过email进行查询
		System.out.println(user);
	}
	//测试插入数据
	@Test
	public void insertUser(){
		User user = new User("root", "Aubrey00");
		int i = userMapper.insertUser(user);
		System.out.println(i);//返回受影响行数
	}
	//测试修改条件
	@Test
	public void testUpdate(){
		int i = userMapper.updateStatus(150, 1);//修改新加入用户的状态
		System.out.println(i);
	}

}
