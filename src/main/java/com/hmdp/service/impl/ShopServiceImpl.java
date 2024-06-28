package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
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
    @Override
    public Result querybyid(Long id) {


        String key=RedisConstants.CACHE_SHOP_KEY + id;
//        从用户的信息中商户的缓存
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            Shop bean = JSONUtil.toBean(s, Shop.class);
            return Result.ok(bean);
        }

//        这里不等于null的时候其实就是对应我们的这个“”
        if (s!=null){
            return Result.fail("店铺不存在");
        }
//        如果查询到的为null
//        此时就要去数据库中查找
        Shop shop= getById(id);

        if (shop==null){
//            解决缓存穿透的问题
//            我们一般的解决的思路是
//            俩种解决：
//            1.在缓存中设置我们的查询不到的为空
//            2.设置布隆过滤器
            stringRedisTemplate.opsForValue().set(key,"");
            stringRedisTemplate.expire(key,RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return Result.fail("不存在");
        }
        String str=JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key,str);
//        设置过期的时间
        stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

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
