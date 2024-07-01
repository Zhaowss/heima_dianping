package com.hmdp.utils;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 缓存三大问题的解决 非工具类方法设计
 * </p>
 *
 * @author 赵巍山
 * @since 2021-12-22
 */
public class redistest extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private StringRedisTemplate stringRedisTemplate;
    public Result querybyid(Long id) {

        String key = RedisConstants.CACHE_SHOP_KEY + id;

        String s = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(s)) {
            Shop bean = JSONUtil.toBean(s, Shop.class);
            return Result.ok(bean);
        }

        if (s != null) {
            return Result.fail("店铺不存在");
        }

        String lockkey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop=null;
        try {
            boolean trylock = trylock(lockkey);
            if (!trylock) {
                Thread.sleep(50);
                querybyid(id);
            }
            shop = getById(id);
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

    public Result queryMuxthrough(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(s)) {
            Shop bean = JSONUtil.toBean(s, Shop.class);
            return Result.ok(bean);
        }
        if (s != null) {
            return Result.fail("店铺不存在");
        }

        String lockkey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop=null;
        try {
            boolean trylock = trylock(lockkey);
            if (!trylock) {
                Thread.sleep(50);
                querybyid(id);
            }
            shop = getById(id);
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
        return Result.ok(shop);
    }


    private  static  final ExecutorService Cache_reBuild_executor= Executors.newFixedThreadPool(10);


    public Result querywithlogic(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(s)) {
            return null;
        }
        if (s != null) {
            return Result.fail("店铺不存在");
        }
        RedisData bean = JSONUtil.toBean(s, RedisData.class);
        JSONObject data = (JSONObject) bean.getData();
        Shop bean1 = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = bean.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return Result.ok(bean1);
        }
        String lockkey = RedisConstants.LOCK_SHOP_KEY + id;

        boolean trylock = trylock(lockkey);
        if (trylock) {
            Cache_reBuild_executor.submit(()->{
                try {
                    saveshoptoRedis(id, 20L);
                }finally {
                    dellock(key);
                }
            });
        }
        if (bean1 ==null){
            return Result.fail("用户为空");
        }
        return Result.ok(bean1 );
    }


    public Shop querybypasthough(Long id) {
        String key=RedisConstants.CACHE_SHOP_KEY + id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            Shop bean = JSONUtil.toBean(s, Shop.class);
            return bean;
        }
        if (s!=null){
            return null;
        }
        Shop shop= getById(id);

        if (shop==null){
            stringRedisTemplate.opsForValue().set(key,"");
            stringRedisTemplate.expire(key,RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        String str=JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key,str);
        stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }


    private  void saveshoptoRedis(Long id,Long expireseconds){
        Shop shop=getById(id);
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireseconds));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    private  boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  void dellock(String key){
        Boolean flag = stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result updateshop(Shop shop) {
        if(shop.getId()==null){
            return Result.fail("登陆的ID为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+shop.getId());
        return Result. ok();
    }
}
