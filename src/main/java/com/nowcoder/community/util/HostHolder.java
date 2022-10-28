package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用于代替session对象(起到容器的作用)
 */
@Component  //加入组件右spring进行管理
public class HostHolder {
    //ThreadLocal:在每个线程中维护一个ThreadLocalMap,map中以ThreadLocal对象为key，value是存入的值
        //因此ThreadLocal的get / set方法是以当前线程为key,以泛型为value进行存储,实现线程隔离
    private ThreadLocal<User> users = new ThreadLocal<>();//存储每个线程所持有的user对象,可以实现线程隔离
    public void setUser(User user) {//在当前线程下持有用户
        users.set(user);
    }
    public User getUser() {//得到当前线程持有的用户
        return users.get();
    }
    public void clear() {
        users.remove();
    }//请求(线程)结束后对当前线程的map进行情况
}
