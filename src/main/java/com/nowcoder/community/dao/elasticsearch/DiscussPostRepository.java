package com.nowcoder.community.dao.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository //ES可以作为特殊的数据库,数据访问层组件,因此使用此注解指定(mapper为mysql专用注解)
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
    //<DiscussPost, Integer>表示该接口需要处理的实体类为DiscussPost,实体类的主键为整型
}
