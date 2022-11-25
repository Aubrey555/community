package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired      //kafka处理分享图片事件:由事件驱动,因此需要生产者对此类型事件进行发布到消息队列
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;//生成图片所要访问的域名(即为本机服务器)

    @Value("${server.servlet.context-path}")
    private String contextPath;//项目的访问名称

    @Value("${wk.image.storage}")
    private String wkImageStorage;//服务器中图片存放的位置(配置文件中获取)

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;//生成图片的访问路径(用户通过此请求路径访问图片)

    /**
     * 服务器处理网页模板生成图片:异步请求处理,返回JSON字符串
     *      同时生成图片的方式一定为异步请求,因为对于内容较多的网页模板,其生成图片所需的时间比较长,采用事件驱动,即创建生成图片分享的主题,
     *      将此分享事件由生产者加入消息队列,由消费者对此事件进行监听处理。
     * @param htmlUrl   传入需要生成图片的路径地址(比如www.baidu.com),网页中直接给出此请求参数
     * @return
     */
    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        // 1.随机生成图片的文件名
        String fileName = CommunityUtil.generateUUID();

        //2.异步生成长图:由kafka消息队列进行处理,生产者发布到指定主题的消息队列,消费者监听此主题,存在事件则处理
        Event event = new Event()   //定义发布的事件,此数据不需要实体类型等数据
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)    //传入需要生成图片的路径地址(比如www.baidu.com)
                .setData("fileName", fileName)  //生成图片的文件名
                .setData("suffix", ".png");     //生成图片的后缀指定
        eventProducer.fireEvent(event);     //发布事件

        //3.返回访问路径:即生成的长图的访问地址
        Map<String, Object> map = new HashMap<>();
            //请求路径: 域名+项目地址+/share/image/+文件名
//        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
            //此时返回给浏览器用户的保存长图的路径地址shareUrl为七牛云服务器的空间地址
        map.put("shareUrl", shareBucketUrl + "/" + fileName);
            //服务器返回响应状态码,以及七牛云服务器存储图片的地址
        return CommunityUtil.getJSONString(0, null, map);
    }

    /** 废弃功能:用户访问七牛云服务器访问网页生成的图片,不再通过保存到本地服务器进行读取
     *      用户通过给定请求路径获取本地服务器生成的图片,直接返回图片
     * @param fileName  得到请求路径中的图片的文件名
     * @param response  用于返回图片信息
     */
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }
        //1.返回内容的类型
        response.setContentType("image/png");//表示为png格式的图片
            //读取本地的图片文件(传入本地的图片地址)
        File file = new File(wkImageStorage + "/" + fileName + ".png");// wkImageStorage->服务器中图片存放的位置(配置文件中获取)
        try {
            OutputStream os = response.getOutputStream();//得到字节输出流
            FileInputStream fis = new FileInputStream(file);//得到图片文件的输入流(此时图片信息即存储在fis中)
            byte[] buffer = new byte[1024];//读取图片的缓冲区,每次最多读取1024字节
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {//fis.read(buffer)实际读取的长度返回给b,如果读取结束则返回b=-1,否则返回实际读取的长度b
                os.write(buffer, 0, b);//此时表示没有结束,则
            }
        } catch (IOException e) {
            logger.error("获取长图失败: " + e.getMessage());
        }
    }
}
