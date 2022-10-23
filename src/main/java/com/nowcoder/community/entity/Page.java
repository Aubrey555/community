package com.nowcoder.community.entity;

import lombok.Data;
import lombok.ToString;

/**
 * @Author:Aubrey
 * @Version:1.0
 * @Date:2022/10/20 19:44
 * @Description: 封装分页功能相关条件
 */
@Data   //生成get set方法
@ToString
public class Page {
    //页面传入
    private int current=1;//页面传入的当前页的页码(默认显示第一页)
    private int limit=10;//页面显示数据的上限(即不设置则每页最多显示10条数据)
    //控制器中服务器进行设置
    private int rows;   //帖子总数(从数据库中查询得到):用于计算总页数(总数/每页上限)
    private String path;//查询路径(即点击分页导航上的页码后跳转到的路径)
    /**
     * 获取当前页的起始行 = current*limit - limit,即表示当前页的起始行应该为第几条数据
     * @return
     */
    public int getOffset(){
        return current*limit - limit;
    }
    /**
     * 获取当前记录的总页数:页面的分页导航处需要显示总页数
     * @return
     */
    public int getTotal(){
        if(rows % limit == 0){//表示当前总贴子数恰好是每页显示数据的倍数,因此一共需要的总页数为
            return rows/limit;
        }else{
            return rows/limit+1;//表示不能被整除,因此+1
        }
    }
    //分页导航中需要显示可见的页码,比如如果当前在第8页,则页面需要显示6789 10页
    /**
     * 获取分页导航中的起始页:比如上面说的第6页,即为当前需要显示的起始页
     *      此时逻辑下,分页导航一共显示5页
     * @return
     */
    public int getFrom(){
        int from = current - 2;//表示当前分页导航的起始页为当前页的前两页
        return from < 1 ? 1 : from;//左侧边界设置(即如果当前页为第1页,则起始页应该显示为1)
    }
    /**
     * 获取分页导航中的结束页:比如前面说的第10页
     * @return
     */
    public int getTo(){
        int to = current+2;
        return to > getTotal() ? getTotal():to;//与前面一致,如果计算的分页导航的结束页大于总页数,则最后显示总页数
    }
}
