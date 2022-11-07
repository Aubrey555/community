package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user")    //该控制器类的访问路径
@Slf4j
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    private String uploadPath;  //获取配置文件中设置服务器对于用户上传文件的存放地址

    @Value("${community.path.domain}")
    private String domain;  //获取配置文件中设置的服务器的域名(此处即使用本机服务器,即上传到本机)

    @Value("${server.servlet.context-path}")
    private String contextPath; //获取当前项目的访问路径

    @Autowired
    private UserService userService;

    @Autowired  //注入HostHolder组件,该组件持有当前请求(线程)对应的用户
    private HostHolder hostHolder;

    @Autowired  //得到当前用户的总点赞数等信息
    private LikeService likeService;

    @Autowired  //实现访问个人主页的功能
    private FollowService followService;
    /**
     * 访问账号设置界面
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    /**
     * 完成头像(文件)上传功能
     * @param headerImage   SpringMVC提供的处理文件的数据类型,如果为多个文件可以使用该类型的数组（name一致时,前端页面上传的文件数据会自动注入到该文件类型中）
     * @param model         向页面返回数据
     * @return
     */
    @LoginRequired      //自定义注解LoginRequired,表示当前方法时候登录才能访问(标注此注解后,只有登录才能访问被标注的控制器方法)
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {  //如果传入文件为空
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";//重新返回到账号设置页面
        }
        //1.不能在服务器中存储原始文件名(多用户存储自己的头像,原始图片名可能会存在相同情况),需要随机生成图片名
        String fileName = headerImage.getOriginalFilename();//得到原始图片的文件名
            //从原始文件名中得到文件格式suffix(即为.之后的内容)
        String suffix = fileName.substring(fileName.lastIndexOf("."));//截取最后一个. 之后的索引
        if (StringUtils.isBlank(suffix)) {//
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        //2.生成随机文件名并存储文件到服务器中
        fileName = CommunityUtil.generateUUID() + suffix;
            // 2.1 得到文件存放的路径uploadPath,文件名为fileName
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 2.2 存储文件(将图片写入到目标文件File中)
            headerImage.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败: " + e.getMessage());//异常日志
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);//统一抛出异常进行处理
        }

        // 3.更新当前用户的头像的路径(web访问路径:服务器域名+项目名(/community)+自定义请求路径，通过此路径获取用户头像,对该自定义请求进行处理即可)
        // http://localhost:8080/community/user/header/xxx.png  下面通过此路径处理获取头像的业务
        User user = hostHolder.getUser();//获取当前用户
        String headerUrl = domain + contextPath + "/user/header/" + fileName;// /user/header/为自定义的控制器访问路径,访问此路径即可获取用户头像,fileName为变量
        userService.updateHeader(user.getId(), headerUrl);//更新用户头像路径

        return "redirect:/index";//重定向到首页访问路径
    }

    /**
     * 获取用户头像的处理
     * @param fileName  请求路径中的参数,表示文件名,可以使用@PathVariable注解获取路径中的参数变量
     * @param response  使用response写出数据
     */
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 1.当前服务器存放文件的路径
        fileName = uploadPath + "/" + fileName;
        // 2.得到文件格式,响应时需要带上
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 3.响应图片到客户端
        response.setContentType("image/" + suffix);//设置响应图片文件的格式
        try (
                FileInputStream fis = new FileInputStream(fileName);//得到图片文件的字节流数据(图片为二进制文件)
                OutputStream os = response.getOutputStream();//得到response的字节输出流,进行输出
        ) {
            byte[] buffer = new byte[1024];//每次输出1024个字节B(即1KB)
            int b = 0;//存储实际输出的大小(即buffer实际存储数据的长度)
            while ((b = fis.read(buffer)) != -1) {//利用while循环进行输出,read()返回实际读取的大小(输出完成后返回-1)
                os.write(buffer, 0, b);//输出buffer中0-b个大小的数据(开始每次都是1024,但最后一次输出b可能小于1024)
            }
        } catch (IOException e) {
            log.error("读取头像失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户密码(新密码和确认新密码输入由前端js页面进行自动判断,此处可以不进行处理)
     * @param oldPsd    原始密码
     * @param newPsd    新密码
     * @param newPsd1   确认密码
     * @param model     向客户端返回数据
     * @return
     */
    @PostMapping("/uploadPsd")
    public String uploadPassword(String oldPsd,String newPsd,String newPsd1,Model model){
        User user = hostHolder.getUser();//获取当前线程的用户
        if(!newPsd.equals(newPsd1)){//新密码和确认密码 不一致时,
            model.addAttribute("NewPsdMsg1","请保证两次密码输入一致!");
            return "/site/setting";//表示修改密码失败,重新返回到修改密码界面
        }

        //1.调用Service层的修改密码
        Map<String, Object> map = userService.updatePwd(user.getId(), oldPsd, newPsd);
        //2.通过map判断是否修改成功(如果修改成功,则跳转到一个中间页面operate-result.html,该页面中会进行自动跳转功能,跳转到首页)
        if(map.isEmpty()){
            model.addAttribute("msg","密码修改成功,请重新登录");
            model.addAttribute("target","/login");//target属性存储激活成功跳转到的页面中的某个链接地址(登陆页面)
            return "/site/operate-result";//表示密码修改成功后,需要跳转的操作结果页面(该页面内会进行自动跳转到/login,即为target携带内容)
        }else{
            //修改失败:即为原始密码  新密码其中一个设置失败,此处不进行判断,都传入请求域中
            model.addAttribute("oldPsdMsg",map.get("oldPsdMsg"));
            model.addAttribute("NewPsdMsg",map.get("NewPsdMsg"));
            model.addAttribute("NewPsdMsg1",map.get("NewPsdMsg1"));
            return "/site/setting";//表示修改密码失败,重新返回到修改密码界面
        }
    }


    /**
     * 实现查看个人主页功能:可以查看任何人的主页
     * @param userId    需要查看的个人主页对应的用户id(通过请求路径传入,在请求方法中通过注解进行解析)
     * @param model     向页面返回参数
     * @return
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        //1.通过传入的userId得到对应的用户
        User user = userService.findUserById(userId);
        //2.判断当前用户是否存在
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //3.返回当前用户到页面
        model.addAttribute("user", user);
        //4.得到当前用户被点赞的数量,并存储到请求域中
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //5.查询当前用户关注(用户)的数量(当前个人主页显示的是关注的用户总数)
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //6.查询当前用户的粉丝的数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        //7.当前登录用户对此时访问的用户,是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        //8.返回到个人主页模板
        return "/site/profile";
    }

}
