package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")    //该控制器类的访问路径
@Slf4j
public class UserController {

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
    @LoginRequired
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
}