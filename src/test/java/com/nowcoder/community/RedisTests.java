package com.nowcoder.community;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    //该类用于测试操作redis中的各类数据
    @Autowired  //注入操作redis的模板文件
    private RedisTemplate redisTemplate;

    @Test   //测试添加String类型数据到redis
    public void testStrings() {
        //1.声明一个key值(自定义的template组件中key值均为String)
        String redisKey = "test:count";
        //2.使用opsForValue()操作String类型数据，使用set存储String类型数据: 1
        redisTemplate.opsForValue().set(redisKey, 1);
            //如果存储String类型数据,且向同一个键存储不同元素,则会覆盖该键之前存储的元素
        //redisTemplate.opsForValue().set(redisKey, 2);
        //3.get()获取key值对应String类型数据
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        //4.increment()增加key值对应String类型数据
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        //5.decrement()减少key值对应String类型数据
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test       //测试操作hash类型数据到redis
    public void testHashes() {
        //1.声明一个key值(自定义的template组件中key值均为String)
        String redisKey = "test:user";
        //2.使用opsForHash()操作哈希类型数据,使用put()方法存储哈希类型元素
                //向redisKey键中存储两组哈希元素
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
        //3.获取key键中给定field对应的value值
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test   //测试操作列表lists类型数据
    public void testLists() {
        String redisKey = "test:ids";
        //1.从列表左侧插入三个元素
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);
        //2.opsForList()得到列表中指定键的大小(即存储元素个数)size(redisKey)
        System.out.println(redisTemplate.opsForList().size(redisKey));//3
        //3.得到列表中指定键对应index下标的元素
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));//103,新进元素都向最左侧插入
        //4.得到列表中指定键的某个范围(0->2)的下标的元素
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));//输出103  102  101
        //5.leftPop()依次从列表左侧弹出三个元素,即为103 ->102 ->101（弹出后列表为空）
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets() {//访问Set集合类型数据
        String redisKey = "test:teachers";
        //1.向Set集合指定键key中添加一些元素
        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "赵云", "诸葛亮");
        //2.获取Set集合指定键中存储元素的个数
        System.out.println(redisTemplate.opsForSet().size(redisKey));
        //3.随机弹出Set集合中指定键内存储的一个元素
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        //4.查看Set集合中指定键内剩余的元素
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public void testSortedSets() {//访问有序集合sort set类型数据
        String redisKey = "test:students";
        //1.向有序集合sort set指定键key中添加一些元素(存储元素及其对应分数,按照分数从小到大有序)
        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);
        //2.统计有序集合指定键内的元素的个数
        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));//5
        //3.得到有序集合指定键中指定元素对应的分数
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒"));//50
        //4.得到有序集合指定键中某个元素的排名(从小到大,reverseRank()倒叙,按照分数从大到小进行排名)
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒"));//4
        //5.得到有序集合指定键中某个范围内的元素(从小到大,第0个 - 第2个)
        System.out.println(redisTemplate.opsForZSet().range(redisKey, 0, 2));//[八戒, 白龙马, 沙僧]
            //按照分数从大到小,倒序进行排名
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));//[悟空, 唐僧, 沙僧]
    }

    @Test
    public void testKeys() {    //对于key的一些全局操作
        //1.删除当前库中某个key对应数据
        redisTemplate.delete("test:user");
        //2.查看当前库中是否存在某个键对应元素
        System.out.println(redisTemplate.hasKey("test:user"));
        //3.设置某个键的过期时间(10s)
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
        //4.查询当前库中以固定模板开头的key（10s后再次访问,则test:students消失,此key键过期）
        Set keys = redisTemplate.keys("test*");
        System.out.println(keys);//[test:count, test:students, test:teachers]
    }

    // 多次访问同一个key时的简化操作
    @Test
    public void testBoundOperations() {
        //上面操作时,对于某一个key重复访问,每次都要将key值传入,十分麻烦,以下进行简化
        String redisKey = "test:count";
        //1.boundValueOps(redisKey)获得固定key的操作对象,该key对应的数据类型为String
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);//初始redisKey对应数据为7
            //对该key对应的数据进行操作operations
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());   //因此返回 12
        //2.boundHashOps获得给定key的操作对象,该key对应的数据类型为Hash
        redisKey = "test:user";
        BoundHashOperations operations1 = redisTemplate.boundHashOps("redisKey");
        DataType type = operations1.getType();
        System.out.println(type);//返回该key对应的数据类型为: HASH
    }

    // redis中的事务管理测试: 编程式事务
    //在redis中一般使用编程式事务进行操作,对于事务的管理范围尽可能缩小,以便在同一个方法内既有事务,也有查询操作。
    // (声明式事务一般直接对一个方法进行操作,但在redis中使用,则在方法内进行查询不会返回结果,而是所有命令结束才返回)
    @Test
    public void testTransactional() {
        //redisTemplate调用execute()执行事务,在该方法内传入一个接口,接口内的public Object execute()方法底层会自动进行调用
            //此时使用匿名内部类对该接口进行实现
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //1.定义key
                String redisKey = "test:tx";
                //2.表示启用事务
                operations.multi();

                    //事务中间的逻辑处理(每个命令不会直接执行,而是放在一个队列中,等到事务提交后统一执行)
                operations.opsForSet().add(redisKey, "zhangsan");
                operations.opsForSet().add(redisKey, "lisi");
                operations.opsForSet().add(redisKey, "wangwu");
                    //事务中间的查询操作无效
                System.out.println("事务中间的查询操作:"+operations.opsForSet().members(redisKey));

                //3.表示提交事务
                return operations.exec();
            }
        });
        //事务提交结束后返回外层execute()方法得到的数据:[1, 1, 1, [wangwu, zhangsan, lisi]]
            //表示每条命令执行后影响的行数,此处添加操作影响1行,返回1
            //以及返回Set集合中所有添加的数据
        System.out.println(obj);
    }


    /**
     * HyperLogLog:统计20万个重复数据的独立总数.(即有重复数据时对重复数据去重)
     */
    @Test
    public void testHyperLogLog() {
        //定义键
        String redisKey = "test:hll:01";//hll即为hyperLogLog
        //存储1-100000中的每个数据i到HyperLogLog类型中(10w个不重复的数据)
        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        //生成1-100000内的任意数据再存储到redisKey键对应的HyperLogLog中
        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }
        //此时存储完毕后本应该存在20w个数据,但HyperLogLog类型存储不重复数据,因此应该为10w大小数据
        //但HyperLogLog为非精确去重,因此估算非重复数据的大小如下:
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    //

    /**
     * HyperLogLog:将3组数据合并, 再统计合并后的重复数据的独立总数.
     *      UV为独立访客含义,统计7天内的UV,也就是将7天内的独立访客数量进行统计
     */
    @Test
    public void testHyperLogLogUnion() {
        //第一组数据:定义key,对应HyperLogLog类型中存储1-10000条数据
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }
        //第二组数据:定义key,对应HyperLogLog类型中存储5001-15000,共10000条数据
        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }
        //第三组数据:定义key,对应HyperLogLog类型中存储10001-20000,共10000条数据
        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }
        //此时一共30000条数据,去重后三组数据具有独立数据一共20000条,进行统计
            //HyperLogLog对多组数据进行去重后进行合并,合并后存储到新的keu中,也对应HyperLogLog类型
        String unionKey = "test:hll:union";//定义合并后的HyperLogLog类型数据对应的键
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);
        //得到去重合并后的大小size(size为估算大小,实际非重复数据大小为20000条)
        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    //演示bitmap:统计一组数据的布尔值
        //不是一种独立的数据结构，实际上就是字符串。对字符串的特殊操作
    @Test
    public void testBitMap() {
        //redisKey键对应的值中存储字符串
        String redisKey = "test:bm:01";

        // 记录数据:将对应数据设置为boolean值
        redisTemplate.opsForValue().setBit(redisKey, 1, true);//设置第一位数据为true
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        // 查询: 查询每个位置Bit对应的值为true还是false
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));//false
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));//true
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));//false

        // 统计: bitCount(redisKey.getBytes())按位统计getBytes()中1(即true)的个数
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);//3:即表示有3位为true
    }

    // 统计3组数据的布尔值, 并对这3组数据做OR(或)运算.
    @Test
    public void testBitMapOperation() {
        //存储第一组数据"test:bm:02":
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);//第一位为true
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);//第二位为true
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);//第三位为true
        //存储第二组数据"test:bm:03":
        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);
        //存储第三组数据"test:bm:04":
        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        //对三组数据进行or运算,得到新的结果,存储到redisKey中
        String redisKey = "test:bm:or";
        //使用RedisConnection对象进行or运算,返回的统计结果为redisKey中true对应元素的大小数量
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                //进行or运算
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                //返回统计结果存储到redisKey中
                return connection.bitCount(redisKey.getBytes());
            }
        });
        //返回统计结果中true对应的数量(一共有7个)
        System.out.println(obj);
        //打印统计结果中的每个元素值
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 6));
    }


}
