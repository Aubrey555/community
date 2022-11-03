package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant { //实现该接口,具有常量值,表示激活状态
    @Resource
    private UserMapper userMapper;
    @Resource
    private LoginTicketMapper loginTicketMapper;//注入LoginTicket类的数据访问层的mapper接口

    @Autowired
    private MailClient mailClient;//邮箱客户端:提供发邮件功能(将发邮件的功能委托给新浪完成,即类似于客户端)
    @Resource
    private TemplateEngine templateEngine;//模板引擎
    //发邮件时要生成一个激活码,激活码包含域名和项目名(都在配置文件中进行了设置)
    @Value("${community.path.domain}")  //从配置文件中获得域名
    private String domain;
    @Value("${server.servlet.context-path}")//从配置文件中获得项目名(community)
    private String contextPath;

    /**
     * 该方法用于进行用户注册,并返回注册账号功能中的用户相关信息
     *      1.首先进行用户信息基本判断,账号 密码 邮箱 等是否已经存在,是否为空(只要不符合直接退出,并设置提示信息)
     *      2.合法后,完善用户信息,并添加到数据库中
     *      3.向用户发送激活账号的邮件(html网页模板),并附带激活链接等信息。
     * @param user  传入需要注册的用户(只具有账号 密码 邮箱三个信息)
     * @return      map封装用户信息
     */
    public Map<String,Object> register(User user){
        Map<String, Object> map = new HashMap<>();

        //1. 空值处理:直接抛出异常
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {//当账号为空时
            map.put("usernameMsg", "账号不能为空!");//提示信息,账号不能为空
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");//密码为空时,返回密码相关提示
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");//邮箱为空时,返回邮箱相关提示
            return map;
        }

        // 关键信息不为空时,验证账号
        User u = userMapper.selectByName(user.getUsername());//通过传入的user,根据用户名查询该用户
        if (u != null) {    //如果数据库中有该用户,表示用户已经存在
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());//通过邮箱查询用户
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 2. 注册用户(上述逻辑都未返回,表示该用户合法,注册即存储用户到数据库)(只具有账号 密码 邮箱三个信息,其他字段还需要设置)
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));//设置该用户的随机字符串(5位),用于MD5密码加密使用
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));//用户给定的密码进行加密(用户给定密码+随机字符串再进行md5加密)
        user.setType(0);//默认为普通用户
        user.setStatus(0);//状态默认为未激活
        user.setActivationCode(CommunityUtil.generateUUID());//该用户需要激活,对应的激活码(由服务器发送)
        //给用户一个随机头像,可以进行修改(占位符%d处的数字为1-1000,牛客网给定,生成随机数1-1000来获取)
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());//用户创建时间
        userMapper.insertUser(user);//将该用户添加到数据库

        // 3. 向用户发送:激活邮件信息 activation.html网页模板
        Context context = new Context();//Thymeleaf包下对象,通过此对象携带向thymeleaf发送的变量
                //传入变量值1    激活邮件中显示的用户邮箱
        context.setVariable("email", user.getEmail());//携带用户的email(id="email",在模板页面activation.html中进行获取)
                //传入变量值2    激活邮件中用户需要点击的激活链接(此链接点击后,访问服务器的/activation请求路径,完成激活,并且传入该用户的id 和 激活码code)
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);//将激活链接传入Context中,即可在Thymeleaf中进行获取
                //调用模板引擎,生成动态网页(HTML格式):传入模板位置(此处为项目提供的模板activation.html)+模板需要数据
        String content = templateEngine.process("/mail/activation", context);

        //调用mailClient工具类的发送邮件功能,向用户邮箱发送主题为"激活账号"的邮件,激活内容(邮件模板)即为content
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map;//返回map信息
    }

    /**
     * 通过传入用户的id和激活码code对用户进行激活(即将status变为1)
     * @param userId    用户id
     * @param code  激活码
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {//初始化时status为0,激活需要变为1,此处为1表示已经激活,则返回重复激活
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {//否则不为1,则激活用户
            userMapper.updateStatus(userId, 1);//激活用户
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;//否则就是不等于,表示激活失败
        }
    }

    /**
     * 该方法标识根据用户id查询用户
     * @param id
     * @return
     */
    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据用户账号密码以及过期时间进行登录
     * @param username  用户账号
     * @param password  登陆密码(此处用户存入的为明文密码,数据库中为MD5密码)
     * @param expiredSeconds    希望的凭证过期时间(记住账号的时间),秒
     * @return      map可以封装多种不同的返回结果
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 1.空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 2.验证账号
        User user = userMapper.selectByName(username);//根据username查询用户
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");//账号注册后需要进行激活
            return map;
        }

        // 验证密码(对用户的密码进行MD5加密后进行比较)
        password = CommunityUtil.md5(password + user.getSalt());//salt为每个用户的随机字符串
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }
        //3. 此时表示用户账号密码均正确,此时既需要生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());//用户的凭证字符为随机字符串
        loginTicket.setStatus(0);//用户状态(0--正常有效,1--过期无效)
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));//设置用户凭证的过期时间
        loginTicketMapper.insertLoginTicket(loginTicket);
        //返回用户凭证的字符
        map.put("ticket", loginTicket.getTicket());//将用户凭证的ticket存入map进行返回
        return map;
    }


    /**
     * 根据用户的id修改密码(控制层中处理表单提交的新密码和确认密码一致的情况)
     *      1.原始密码和新密码空值处理
     *      2.确认原始密码是否正确
     *      3.确认新密码长度是否合规
     *      4.修改新密码
     * @param userId    用户id
     * @param oldPsd   用户原始密码
     * @param newPsd   用户新密码
     * @return
     */
    public Map<String,Object> updatePwd(int userId,String oldPsd,String newPsd){
        Map<String, Object> map = new HashMap<>();
        //1.空值处理
        if(StringUtils.isBlank(oldPsd)){
            map.put("oldPsdMsg", "原始密码不能为空!");
            return map;
        }
        if(StringUtils.isBlank(newPsd)){
            map.put("NewPsdMsg", "新密码不能为空!");
            return map;
        }
        //2.校验原始密码
        User user = userMapper.selectById(userId);
        System.out.println(user);
        String password = CommunityUtil.md5(oldPsd + user.getSalt());//salt为每个用户的随机字符串,得到用户输入的密码
        if(!user.getPassword().equals(password)){
            map.put("oldPsdMsg", "原始密码输入错误!");
            return map;
        }

        //3.长度判断
        if(StringUtils.length(newPsd) < 8){
            map.put("NewPsdMsg", "新密码长度至少为8位,请重新进行设置!");
            return map;
        }
        //4.更新密码(更新密码为MD5加密的密码)
        password = CommunityUtil.md5(newPsd + user.getSalt());
        userMapper.updatePassword(userId, password);//用户更新后的密码
        return map;
    }

    /**
     * 实现退出功能:通过用户凭证设置凭证表的用户状态为1,表示用户退出
     * @param ticket
     */
    public void logout(String ticket) {
        loginTicketMapper.updateStatus(ticket, 1);
    }

    //根据ticket凭证获得对应实体类LoginTicket
    public LoginTicket findLoginTicket(String ticket) {
        return loginTicketMapper.selectByTicket(ticket);
    }

    //通过用户的userId更新用户的头像路径headerUrl
    public int updateHeader(int userId, String headerUrl) {
        return userMapper.updateHeader(userId, headerUrl);
    }

    //通过用户名查找用户
    public User findUserByName(String toName) {
        return userMapper.selectByName(toName);
    }
}
