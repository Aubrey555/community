package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/24 14:31
 * @Description: 用于实现登录注册功能的控制器
 */
@Slf4j
@Controller
public class LoginController  implements CommunityConstant {

    @Autowired
    private UserService userService;
    @Value("${server.servlet.context-path}")
    private String contextPath;//项目路径,从配置文件中获取
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 通过客户端传入的参数,调用Service层的用户注册功能,完成用户注册
     * @param model 向客户端返回的数据Model
     * @param user  客户端之传入username password email,自动封装到User(id一致)中
     * @return
     */
    @PostMapping("/register")
    public String register(Model model, User user){
        //1.调用Service层的用户注册功能,完成用户注册,返回map(map中可能存储注册失败的提示信息)
        Map<String, Object> map = userService.register(user);

        //2.通过map判断是否注册成功(如果注册成功,则跳转到一个中间页面operate-result.html,该页面中会进行自动跳转功能,跳转到首页)
        if(map.isEmpty()){
            model.addAttribute("msg","注册成功,已经向您的邮箱"+user.getEmail()+"发送激活邮件,请尽快激活!");//提示信息
            model.addAttribute("target","/index");//target属性存储激活成功跳转到的页面中的某个链接地址
            return "/site/operate-result";//表示注册成功后,需要跳转的操作结果页面(该页面内会进行自动跳转到主页/index,即为target携带内容)
        }else{
            //注册失败:即为邮箱 账号 密码其中一个设置失败,此处不进行判断,都传入请求域中
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";//此时表示注册失败,返回到注册界面
        }
    }

    /**
     * 完成用户 点击激活邮件 中激活请求的响应,并返回激活结果
     *      http://localhost:8080/community/activation/101/code,表单中激活请求的路径,携带激活用户的id,以及该用户的随机激活码
     * @param model
     * @param userId
     * @param code
     * @return
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)   //不是表单提交,单纯链接,因此使用GET请求
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");//激活成功,跳转到登录界面(此处请求域携带target属性,在operate-result统一完成跳转)
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");//重复激活,跳转到首页
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");//激活失败,会从operate-result.html跳转到主页
        }
        return "/site/operate-result";  //返回到operate-result.html页面,会携带Model请求域的数据
    }

    //跳转到注册页面,进行用户注册
    @GetMapping("/register")
    public String getRegisterPage(){
        return "/site/register";
    }
    //跳转到登录页面,准备进行用户登录
    @GetMapping("/login")
    public String getLoginPage(){
        return "/site/login";
    }

    @Autowired
    Producer kaptchaProducer;   //注入配置的kaptcha组件,用于生成验证码图片

    /**重构此方法,将生成的验证码存入redis中
     * 该请求用于返回一个验证码图片
     * @param response  通过response对象向浏览器输出验证码图片
     * //@param session   服务器需要记录当前生成的验证码,从而在登陆时对该验证码进行验证(不能使用Cookie将验证码记录在浏览器中,容易被破解,因此使用session记录敏感信息在服务器中)
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 1.生成验证码
        String text = kaptchaProducer.createText(); //自动生成一个4位的随机字符串(配置类中进行了设置)
        BufferedImage image = kaptchaProducer.createImage(text);//通过此字符串生成一个对应图片,类型为BufferedImage
//        // 2.将验证码存入session:从而在客户登录时进行验证
//        session.setAttribute("kaptcha", text);

        //2.将验证码存入redis:从而在客户登录时进行验证
            //设置当前验证码的归属(拥有者),登录时需要获得此归属从而生成键key,来确认验证码
        String kaptchOwner = CommunityUtil.generateUUID();//某用户正在登陆,因此生成验证码,但不知此用户是否存在,因此此信息表示当前准备登录用户的凭证
        Cookie cookie = new Cookie("kaptchOwner",kaptchOwner);//将此凭证加入Cookie给客户端进行保存
        cookie.setMaxAge(60);//有效时间60s
        cookie.setPath(contextPath);//有效路径为整个项目
        response.addCookie(cookie);
            //存储此验证码到redis中
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchOwner);
            //以key为键,验证码text为值,验证码储时间为60s
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        // 3.将图片输出给浏览器:声明向浏览器返回的数据的格式
        response.setContentType("image/png");//向浏览器返回png格式的图片数据
        try {
            OutputStream os = response.getOutputStream();   //获取输出的图片的字符流(SpringMVC维护,自动关闭)
            ImageIO.write(image, "png", os);//使用工具类ImageIO将该图片进行输出:传入输出内容+输出的格式+输出的流
        } catch (IOException e) {
            log.error("响应验证码失败:" + e.getMessage());//如果有问题就日志记录
        }
    }

    /** 重构此方法:再redis中查看是否存在当前输入的验证码
     * 用于处理用于登录请求,请求方式为Post(表单提交)
     * @param username  用户名
     * @param password  用户密码
     * @param code      用户输入的验证码
     * @param rememberme    即登录表单上的记住我按钮,如果选中此用户则服务器可以较长时间记住此用户;没有勾上此记住时间较短
     * @param model     请求域中存储数据
     * @param session   服务器在页面加载时生成验证码,并且会记录在session中,因此获取session,取出其中的验证码和用户输入的code进行比较验证
     * @param response   登陆成功,生成该用户的唯一ticket字符,需要用Cookie进行保存,并穿到浏览器保存,因此使用response添加cookie
     * @param kaptchOwner 从当前浏览器的Cookie中获得验证码的拥有者
     * @return      返回到登录成功/失败的界面
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response,@CookieValue("kaptchOwner") String kaptchOwner) {
//        //1. 先检查验证码
//        String kaptcha = (String) session.getAttribute("kaptcha");//在getKaptcha()请求方法中,对验证码进行了保存

        //1.此时从redis中获得验证码
        String kaptcha = null;//验证码
        if(StringUtils.isNotBlank(kaptchOwner)){ //kaptchOwner不为空,表示此验证码拥有归属者
            //获得验证码对应的键key
            String redisKey =  RedisKeyUtil.getKaptchaKey(kaptchOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";//重新返回到登陆界面
        }

        //2. 检查账号,密码(没有勾上记住我,则过期时间短,勾上了则过期时间设置的比较长)
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;//该常量表示浏览器记住该用户账号 密码多少天
        Map<String, Object> map = userService.login(username, password, expiredSeconds);//进行登录,返回map,通过map进行判断
        if (map.containsKey("ticket")) {//只有登陆成功才向map中放入ticket凭证
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());//将用户凭证的字符ticket存储到Cookie,发送到浏览器
            cookie.setPath(contextPath);//Cookie的有效路径,即为当前项目(contextPath是通过获取配置文件属性内容进行自动注入获得)
            cookie.setMaxAge(expiredSeconds);//设置此Cookie的有效时间
            response.addCookie(cookie);//发送Cookie到页面,响应时就会发送
            return "redirect:/index";//登陆成功,返回到主界面,请求重定向(携带数据)
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";//登陆失败,返回到登陆界面
        }
    }

    /**
     * 完成退出功能
     * @param ticket    传入该用户的ticket凭证字符
     * @return
     */
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();//退出后请求认证信息
        return "redirect:/login";   //退出后,请求重定向到登陆页面,默认为get请求(重新登陆)
    }

}
