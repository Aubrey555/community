package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller     //该组件用于通过页面传入的关键字基于ES服务器进行全局搜索
public class SearchController implements CommunityConstant {

    @Autowired  //该业务层组件用于全局搜索服务
    private ElasticsearchService elasticsearchService;

    @Autowired  //用于得到帖子对应的作者
    private UserService userService;

    @Autowired  //得到帖子的点赞数量
    private LikeService likeService;


    /**
     * 实现通过传入关键字keyword返回搜索结果
     * @param keyword   搜索的关键字      search?keyword=xxx，搜索参数拼接到请求路径
     * @param page      搜索得到结果的分页对象
     * @param model     返回到页面的model数据
     * @return          返回到查询结果页面
     */
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 1.搜索帖子(得到搜索结果Page,封装所有搜索数据DiscussPost实体),得到搜索数据Page<DiscussPost>
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        // 2.处理查询得到的数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();//维护需要在页面上查询的所有帖子的所有数据
        if (searchResult != null) {
            //searchResult中封装的每个数据都是DiscussPost帖子实体类,为搜索得到的帖子对象,进行处理
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();//维护每个帖子需要展现在页面的数据
                // 当前帖子对象
                map.put("post", post);
                // 当前帖子的作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 当前帖子的点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);//封装map到discussPosts
            }
        }
        // 3.页面返回查询得到的数据
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        // 4.分页信息
        page.setPath("/search?keyword=" + keyword);//分页导航中每页点击跳转的路径即为path
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());//设置分页的总行数

        //5.返回到搜索界面
        return "/site/search";
    }

}
