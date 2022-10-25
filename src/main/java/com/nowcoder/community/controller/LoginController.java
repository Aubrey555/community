package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/24 14:31
 * @Description: 用于实现登录注册功能的控制器
 */
@Controller
public class LoginController  implements CommunityConstant {

    @Autowired
    private UserService userService;


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
    //跳转到登录页面,进行用户登录
    @GetMapping("/login")
    public String getLoginPage(){
        return "/site/login";
    }

}
