package com.hmdp.service.impl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {



    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private  RedisUtil redisUtil;
    @Override
    public Result querybyid(Long id) {

//        1.设置空或者布隆解决缓存的穿透的问题
        Shop shop = redisUtil.getpassthough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        2.互斥锁解决缓存的击穿
//        Result shop = redisUtil.querybyid(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        3.设置逻辑过期解决缓存的击穿
//         Shop shop = redisUtil.querywithlogic(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop==null){
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);

    }



    @Override
    @Transactional
    public Result updateshop(Shop shop) {
//        更新数据库
//        先操作数据库之后再操作我们的缓存
        if(shop.getId()==null){
            return Result.fail("登陆的ID为空");
        }
         updateById(shop);
//        删除缓存
         stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+shop.getId());
         return Result. ok();
    }
}
