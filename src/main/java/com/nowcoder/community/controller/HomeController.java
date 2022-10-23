package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/19 18:58
 * @Description: 控制器类:完成首页跳转功能
 */
@Controller
@RequestMapping("/community")   //访问路径前缀,如果加上则请求路径必须带/community
public class HomeController {
    @Autowired  //注入组件
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    /**
     *
     * @param model   Model中封装服务器获取的相关数据,传到客户端页面进行读取
     * @param page    Page用于封装分页的相关数据,客户端调用该方法时,先传入相关部分参数封装到Page(在此方法中,再对Page进行设置参数)
     * @return
     */
    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getindexPage(Model model, Page page) {
        //0.读取数据库中帖子表的数据总数,即为分页数据的总行数
        page.setRows(discussPostService.findDiscussPostRows(0));
        //1.每页显示的数据limit(默认为10),以及当前页码current均由页面传入,并赋值给Page
        page.setPath("index");//查询路径(即点击前端的分页功能的每个跳转页面的链接,比如上一页,下一页等),都跳转到此"index"页面

        //1.查询所有用户(userId=0)的帖子(比如:每页显示10条,显示第一页),传入参数:当前页的起始行,以及每页显示的条数
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        //2.因为上述集合list中返回的是每个用户User的userid,因此需要定义新集合,通过userId得到User对象
            //list中的每一个map中存放两个键值对:"post"->post,"user"->User(即该帖子对应的用户).
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if(list!=null){//如果帖子不为空,遍历帖子
            for (DiscussPost post:list){
                HashMap<String, Object> map = new HashMap<>();
                map.put("post",post);
                User user = userService.findUserById(post.getUserId());
                map.put("user",user);
                discussPosts.add(map);//将map集合加入到discussPosts中
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        //SpringMVC会自动创建Model和Page,并将Page注入到请求域Model中,因此可以直接在页面中访问Page对象的数据
        //则此时不需要将Page传入Model中
        //model.addAttribute("page",page);
        return "index";
    }

}