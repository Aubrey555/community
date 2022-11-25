package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service    //业务逻辑层:用于进行网站数据统计,比如UV(独立访客)和DAU(日活用户)
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;//使用RedisTemplate对Redis进行操作
    //格式化日期模板形式
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    /**
     * 将指定的IP计入UV,最后用于统计网站的某个日期的独立访客
     * @param ip    每次访问网站的用户ip(不论登录用户或匿名用户)
     */
    public void recordUV(String ip) {
        //将当前的日期格式化传入getUVKey(),得到UV对应的key
            //传入当前时间,即可统计得到当前日期所有的独立访客UV
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        //向redis中记录redisKey对应的ip地址。value类型为:HyperLogLog
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    /**
     * 统计指定日期范围内的UV,即独立访客数量
     *      (即每天都有独立访客存储在redisKey对应的值中,此时需要统计start->end日期中所有独立访客的数量,HyperLogLog会进行去重)
     * @param start     开始日期
     * @param end       结束日期
     * @return      返回独立访客数量
     */
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        //1.整理该日期范围内的key(遍历start->end日期内的所有key,存储在集合keyList中)
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();//得到一个实例化日期
        calendar.setTime(start);//开始日期为start
        while (!calendar.getTime().after(end)) {//只要calendar的时间不晚于,也就是小于等于end,则进行while循环,对key进行统计
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));//得到当前日期getTime()对应的key
            keyList.add(key);//存储到redisKey集合中
            calendar.add(Calendar.DATE, 1);//当前日期+1天
        }

        //2.合并keyList集合中所有key对应的集合中的数据,存储在redisKey中(即为start->end中的所有独立UV)
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());//得到给定日期范围存储的所有用户,存储到redisKey中

        //3.返回redisKey-》HyperLogLog统计的独立UV的结果,即总数
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    /**
     * 将指定用户计入DAU:即日活跃用户
     *      使用Bitmap统计日活用户的方式:通过用户ID进行统计日活用户,
     *      比如对于ID=101的用户如果登录,则在Bitmap的101位置存储1,表示为一个日活用户;
     *      对于ID=202的用户如果登录,则在Bitmap=202位置存储1.
     * @param userId    登陆的一个用户id
     */
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);//调用该方法,则用户默认登录,即为一个活跃用户
    }

    /**
     * 统计指定日期范围内的DAU:类似于统计UV
     *      得到给定日期范围(start,end)内,只要用户登陆过一次，即为活跃用户
     *      用户登陆过一次,即在redisKey对应的值中,userId位置上的元素为1,则表示此日期用户进行过登录,则为一个活跃用户
     * @param start
     * @param end
     * @return      返回活跃用户总数
     */
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        //1.整理该日期范围内的key,给定日期范围对应的key都存入keyList中
            //类似于得到指定日期的UVcalculateUV(Date start, Date end)
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        //2.进行OR运算:一真为真,对应位置为true则表示该用户在给定日期(start,end)中为一个活跃用户
            //返回活跃用户总数
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                //生梦新的key,存储给定日期中所有活跃用户的总数
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }
}
