package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {//当前6.4.3版本的ES某些功能在升级到版本7后就不再使用了,可看官网具体介绍
    //ES搜索引擎的数据来源为Mysql,从Mysql中获得,再转存到ES搜索引擎中进行搜索。因此需要注入DiscussPostMapper,从数据库中得到所有帖子数据
    @Resource
    private DiscussPostMapper discussMapper;

    //向ES服务器中插入数据需要用到ElasticsearchRepository接口,因此得到实现该接口的子接口
    @Autowired
    private DiscussPostRepository discussRepository;

    //当DiscussPostRepository对某些特殊情况下的数据不能处理时,使用ElasticsearchTemplate完成
    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    //测试向ES服务器中插入数据(通过mysql查询得到数据,数据在ES中存放的索引以及对应类型,在该数据对应的实体类中使用注解进行了指定)
    //对于此DiscussPost帖子类数据,自动创建indexName = "discusspost"索引进行存放
    @Test
    public void testInsert() {
        //通过id(241 242 243)得到mysql中存放的数据,并插入ES服务器指定索引中(索引没有会自动进行创建)
        discussRepository.save(discussMapper.selectDiscussPostById(241));//save方法插入一条数据
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test   //saveAll()方法插入多条数据
    public void testInsertList() {
        //传入多条数据(数据集合):表示插入101用户的100条数据对应的List集合(可能没有)
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100));
    }

    //修改ES服务器中id对应的数据
    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
        // discussRepository.deleteById(231);//删除id=231在此索引中的数据
        discussRepository.deleteAll();//删除索引中的所有数据
    }

    /**
     * 通过discussRepository接口实现利用ES服务器全文搜索数据,实现:
     *      按条件搜索:搜索关键词构造
     *      排序方式构造
     *      搜索结果高亮显示(即在搜索结果前后加上标签,在html网页中即可高亮显示颜色)
     */
    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()    //该实现类用于构造搜索条件
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))//multiMatchQuery()方法表示即从title中又从content中搜索text=互联网寒冬
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) //fieldSort()构造排序条件:优先按照type(置顶/不置顶)排序,倒序则置顶在前面
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))    //再按照score帖子分数进行排序
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))   //最后按照帖子时间排序,最新的在前面
                .withPageable(PageRequest.of(0, 10))    //PageRequest.of()表示分页查询条件,0表示从首页开始查询,size=10表示一页最多10条数据
                .withHighlightFields(       //withHighlightFields表示对哪些词进行高亮显示(即设置标签)
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),  //指定高亮显示的字段"title",前后标签为<em>
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>") //指定高亮显示的字段"title",前后标签为<em>
                ).build();//build()方法执行后,即可返回SearchQuery接口的实现类

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // 底层获取得到了高亮显示的值, 但是没有返回.

        //discussRepository.search(searchQuery)返回一个Page对象,page对象中封装了实体类集合,即多条数据
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements());//得到一共有多少条数据
        System.out.println(page.getTotalPages());//返回一共多少页
        System.out.println(page.getNumber());//当前为第几页
        System.out.println(page.getSize());//每页最多显示多少条数据
        for (DiscussPost post : page) {//遍历page,返回帖子数据
            System.out.println(post);
        }
    }

    /**
     * 通过elasticTemplate实现利用ES服务器全文搜索数据,实现:
     *      按条件搜索:搜索关键词构造
     *      排序方式构造
     *      搜索结果高亮显示(即在搜索结果前后加上标签,在html网页中即可高亮显示颜色)
     */
    @Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        //利用elasticTemplate进行查询
        //queryForPage得到的searchQuery结果交给SearchResultMapper()接口进行处理,最后返回封装所有搜索结果的Page对象
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                //1.通过response得到所有搜索命令的多条数据SearchHits
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }
                //2.将搜索得到的所有数据集合hits进行处理,处理结束后封装到List集合
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    //每得到一个实体类的数据hit,就将该数据包装到实体类DiscussPost中
                    DiscussPost post = new DiscussPost();
                    //从hit中的map集合中得到每一个字段的数据
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));//得到该实体类的主键id

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));//该帖子的用户id

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);//该帖子的标题(原始标题)

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);//该帖子的内容(原始内容)

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));//该帖子的状态

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));//该帖子的创建时间

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));//该帖子的评论数量

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");//得到title标题中高亮显示的内容
                    if (titleField != null) {   //titleField不为空表示标题中有高亮显示的内容
                        post.setTitle(titleField.getFragments()[0].toString()); //得到标题中第一个高亮的内容,并覆盖原始title中的内容
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");//得到content中高亮显示的内容
                    if (contentField != null) { //不为空
                        post.setContent(contentField.getFragments()[0].toString());//得到内容中第一个高亮的部分,并覆盖原始content的内容
                    }

                    list.add(post);
                }
                //返回AggregatedPage接口的一个实现类AggregatedPageImpl(按照如下传入即可)
                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

}
