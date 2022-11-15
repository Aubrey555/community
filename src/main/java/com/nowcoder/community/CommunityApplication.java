package com.nowcoder.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
@MapperScan("com.nowcoder.community.dao")	//主配置类上加@MapperScan注解，则其他的mapper就可以不用标注@Mapper注解
public class CommunityApplication {

	@PostConstruct	//由该注解修饰的方法,会在构造器初始化结束后执行,此处即在SpringBoot项目启动后执行
	public void init(){
		//该方法用于解决ES搜索引擎和Redis对于Netty使用冲突的问题(主要是es搜索引擎底层的问题)
		System.setProperty("es.set.netty.runtime.available.processors","false");
	}

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
