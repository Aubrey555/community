package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired  //用于caozuoredis数据库
    private RedisTemplate redisTemplate;

    /**
     * 实现点赞的业务方法(该业务中不仅需要实现当前用户对于实体entityType的点赞功能,并且要实现统计实体对应的用户的点赞数量统计(即发帖人的帖子点赞数的统计))
     *      两个业务功能,通过redis学习,使用编程式事务解决
     * @param userId    当前点赞的用户
     * @param entityType    点赞的实体(帖子？评论?)
     * @param entityId      实体的id(哪一条帖子?评论?)
     * @param entityUserId  点赞实体所对应的用户(即发布帖子/评论/评论的回复->对应的用户),即被赞的用户
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //1.向实体entityType中存储数据,此处通过工具类得到该实体对应的key键(向此实体对应的key的Set集合中存储元素)
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);//即当前用户为点赞人
                //2.实体(帖子/评论/评论的回复)对应的用户id传入后对应的key键
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);//即为被赞人
                //3.判断当前用户userId是否向entityLikeKey键内存储过元素(点击一次为点赞,两次为取消点赞.因此判断当前用户是否点过赞)
                    //提前进行查询:即当前集合内是否存在userId(redis中的事务是将所有命令放在队列中,事务提交后统一进行查询)
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                    //开启事务
                operations.multi();
                //4.如果isMember为真,表示entityLikeKey键对应的Set集合存储过此用户,即此用户已经点过赞
                if (isMember) {
                    //则第二次点赞实现取消点赞功能,即将userId在entityLikeKey对应的Set集合内取消
                    operations.opsForSet().remove(entityLikeKey, userId);
                    //为发帖人(或者发评论/发评论回复)对应的userLikeKey键中的数据+1,表示点赞数+1(String类型数据)
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    //否则,userId不在此集合内,表示当前用户没点过赞,则添加此用户userId到集合内,表示一个点赞用户
                    operations.opsForSet().add(entityLikeKey, userId);
                    //否则为发帖人的点赞数-1
                    operations.opsForValue().increment(userLikeKey);
                }
                //5.提交事务
                return operations.exec();
            }
        });
    }

    /**
     * 查询某实体(帖子/评论)点赞的数量:在界面即显示此实体被点赞的数量
     * @param entityType    实体类的类型(帖子/评论)
     * @param entityId      实体类对应id
     * @return
     */
    public long findEntityLikeCount(int entityType, int entityId) {
        //通过传入实体类的类型和id得到实体类的键值
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //返回entityLikeKey键对应Set集合内的点赞数量(即存储元素数量)
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /**
     * 查询某用户userId对某实体entityType的点赞状态
     *      即在该实体对应的键entityLikeKey对应的Set集合中查看是否有userId元素
     * @param userId
     * @param entityType
     * @param entityId
     * @return          返回1表示用户userId对此实体类进行了点赞,否则没有点赞(为0)
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    /**
     * 查询某个用户userId获得的总的点赞(会在个人主页进行显示)
     * @param userId
     * @return      返回总的点赞数
     */
    public int findUserLikeCount(int userId) {
        //1.得到当前用户在redis中的键key(存储的数据类型为String)
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        //2.得到该键在redis中的value值
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        //3.返回点赞总数
        return count == null ? 0 : count.intValue();
    }
}
