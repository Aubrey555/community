package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller //该控制器用于处理统计UV和DAU的请求
public class DataController {

    @Autowired
    private DataService dataService;

    // 打开统计页面:即访问统计DAU和UV的界面
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    /**
     * 统计网站UV
     * @param start     统计UV的开始时间,使用@DateTimeFormat(pattern = "yyyy-MM-dd")注解给定日期格式
     * @param end       统计UV的结束时间
     * @param model     返回模板的数据
     * @return
     */
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        //1.得到给定日期范围的uv数据
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);//统计的uv数据(独立访客)结果
        model.addAttribute("uvStartDate", start);//开始日期返回模板,用于返回结果时再次进行显示
        model.addAttribute("uvEndDate", end);//结束日期也返回模板
        return "forward:/data"; //转发到/data请求,即再次访问模板界面
    }

    /**
     * 统计活跃用户DAU
     * @param start     统计开始时间start
     * @param end       统计结束时间end
     * @param model     返回模板的数据
     * @return
     */
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
}
