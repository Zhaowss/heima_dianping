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


        Shop shop= getById(id);

        if (shop==null){
            return Result.fail("不存在");
        }
        String str=JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key,str);

        return Result.ok(shop);



    }
}
