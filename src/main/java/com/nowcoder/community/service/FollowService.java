package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service    //该业务组件中实现关注相关逻辑
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    /**
     * 某用户user 对某实体entityType(实体id为entityId)进行关注
     *      存储两份数据,因此需要保证事务(使用编程式事务)
     *      1.被关注实体(followerKey)的粉丝数+1,即向键对应的value值中存储元素
     *      2.关注者(followeeKey)的关注数+1，即向键对应的value值中存储一份元素
     * @param userId    当前用户
     * @param entityType    被关注的实体
     * @param entityId      被关注的实体id
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //1.得到关注者的关注实体对应的键key,存储关注者的所有关注实体
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                //2.得到被关注者对应的键key,存储被关注者的所有关注粉丝
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //3.开启事务
                operations.multi();
                    //3.1 向关注者对应的键中添加数据,存储关注者所有的关注对象
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                    //3.2 向被关注者对应的键中添加数据,存储被关注者所有的粉丝
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                //4.提交事务
                return operations.exec();
            }
        });
    }

    /**
     * 实现取消关注的功能
     *      某用户user对于某个实体entity取消关注,两个操作,仍需要开启事务
     *      1.被关注实体(followerKey)的粉丝数减1,即在key对应的ZSet集合中移除实体对应的id
     *      2.关注者(followeeKey)的关注数-1,即在key对应的ZSet集合中移除对应id
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }

    /**
     * 查询某用户user关注的实体的数量
     * @param userId    用户user
     * @param entityType    关注的实体(即关注多少个帖子/用户等)
     * @return
     */
    public long findFolloweeCount(int userId, int entityType) {
        //获取某个用户user关注的实体对象 对应的键
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        //得到key对应集合的数据量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * 查询实体的粉丝的数量
     * @param entityType    某个帖子/课程/用户(当前项目一般都是用户的粉丝数)
     * @param entityId      某帖子/课程/用户对应的id
     * @return
     */
    public long findFollowerCount(int entityType, int entityId) {
        //得到实体对应的键
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        //键key中数据的大小
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**
     * 查询当前用户是否已经关注此实体(一般即为用户)
     *      1.即先获取某个用户user关注的实体对象 对应的键
     *      2.在键key对应的集合ZSet中查看是否含有该实体对象entityId
     * @param userId
     * @param entityType
     * @param entityId      以id作为分数,在有序集合ZSet集合中排序
     * @return
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        //1.即先获取某个用户user关注的实体对象 对应的键
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        //2.在键key对应的集合ZSet中查看是否含有该实体对象,entityId作为分数
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 实现查询某用户关注的人
     * @param userId    判断当前用户
     * @param offset    分页数据:即为第1行数据开始查
     * @param limit     分页数据:(mysql中为当前页显示的数量)
     * @return      返回集合中的map数据,封装了关注的用户和关注的时间
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        //1.得到redis中某个用户user关注的实体对象(用户) 对应的键
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        //2.(倒叙查询,最新的数据在前)传入参数为: 键 开始索引  截止索引(最大数-1)
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        //3.得到所有的用户
        if (targetIds == null) {
            return null;
        }
            //待返回的集合数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            //根据targetId得到关注的用户user
            User user = userService.findUserById(targetId);
            map.put("user", user);
            //通过键 user参数,得到关注此用户对应的时间score
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        //4.返回关注的时间
        return list;
    }

    /**
     *  查询某用户的粉丝列表集合
     * @param userId    用户id
     * @param offset    分页数据
     * @param limit     分页数据
     * @return
     */
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        //1.通过用户id获取某个实体(entityType)所拥有的粉丝集合Set
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        //2.得到该键从offset->最后一条数据的粉丝数的集合(ZSet集合存储有序元素,因此返回的set集合元素有序)
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }
        //3.存储粉丝数据到List中
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        //4.返回
        return list;
    }

}
