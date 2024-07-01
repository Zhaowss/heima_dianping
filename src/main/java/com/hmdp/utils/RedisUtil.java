package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @ClassNAME RedisUtil
 * @Description
 * 基于StringRedisTemplate封装一个缓存工具类，满足下列需求：
 * 方法1：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
 * 方法2：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
 * 方法3：根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
 * 方法4：根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
 * @Author zhaoweishan
 * @Date 2024/6/30 10:07
 * @Version 1.0
 */


@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private  static  final ExecutorService Cache_reBuild_executor= Executors.newFixedThreadPool(10);
    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public  void set(String key,Object object, Long time, TimeUnit timeUnit){
//     将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(object));
        stringRedisTemplate.expire(key,time, timeUnit);
    }
    public  void logicttlset(String key,Object object, Long time, TimeUnit timeUnit){
//      缓存击穿的逻辑过期的逻辑设置方法
        RedisData redisData=new RedisData();
        redisData.setData(object);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R getpassthough(String myprestr, ID id, Class<R> myclass, Function<ID,R> mufunc,Long time, TimeUnit timeUnit){
//        第一种利用查询缓存中插入空数据避免缓存的穿透
        String key=myprestr+id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            R bean = JSONUtil.toBean(s, myclass);
            return bean;
        }
        if (s!=null){
            return null;
        }
        R shop=mufunc.apply(id);

        if (shop==null){
            stringRedisTemplate.opsForValue().set(key,"");
            stringRedisTemplate.expire(key,time,timeUnit);
            return null;
        }
        String str=JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key,str);
        stringRedisTemplate.expire(key,time, timeUnit);
        return shop;

    }


    public <R,ID> R querywithlogic(String myprestr, ID id, Class<R> myclass, Function<ID,R> mufunc,Long time, TimeUnit timeUnit) {
//      缓存的击穿的逻辑过期的方法实现
        String key = myprestr+ id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(s)) {
            return null;
        }
        RedisData bean = JSONUtil.toBean(s, RedisData.class);
        JSONObject data = (JSONObject) bean.getData();
        R bean1 = JSONUtil.toBean(data,myclass);
        LocalDateTime expireTime = bean.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return bean1;
        }
        String lockkey =myprestr+ id;

        boolean trylock = trylock(lockkey);
        if (trylock) {
            Cache_reBuild_executor.submit(()->{
                try {
                    saveshoptoRedis(id, 20L,mufunc);
                }finally {
                    dellock(key);
                }
            });
        }
        if (bean1 ==null){
            return null;
        }
        return bean1;
    }
    private<ID,R>  void saveshoptoRedis(ID id,Long expireseconds, Function<ID,R> mufunc){
//      数据保存为逻辑过期的时间
        R shop=mufunc.apply(id);
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        // 查询完毕设置为逻辑过期
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireseconds));
//        写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }


    private  boolean trylock(String key){
//        获得分布式的锁  setnx命令
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  void dellock(String key){
//        释放锁    del
        Boolean flag = stringRedisTemplate.delete(key);
    }


    public <R,ID> Result querybyid(String myprestr, ID id, Class<R> myclass, Function<ID,R> mufunc,Long time, TimeUnit timeUnit)  {
//        互斥锁解决缓存的击穿
        String key = myprestr + id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(s)) {
            R bean = JSONUtil.toBean(s, myclass);
            return Result.ok(bean);
        }
        if (s != null) {
            return Result.fail("店铺不存在");
        }
        String lockkey =myprestr+ id;
        R shop=null;
        try {
            boolean trylock = trylock(lockkey);
            if (!trylock) {
                Thread.sleep(50);
                querybyid(myprestr,id, myclass,  mufunc,time, timeUnit);
            }
            shop = mufunc.apply(id);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key, "");
                stringRedisTemplate.expire(key, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String str = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(key, str);
            stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            dellock(key);
        }
        if (shop==null){
            return Result.fail("用户为空");
        }
//
        return Result.ok(shop);
    }


}
