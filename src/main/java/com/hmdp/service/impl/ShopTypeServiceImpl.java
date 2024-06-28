package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {



    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public List<ShopType> querylist() {
        List<ShopType> list=new ArrayList<ShopType>();
//        首先会得到我们在redis中存放Map的数据
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisConstants.ShopType);
//      首先获取到这个对应的map
        if (!entries.isEmpty()){
            Collection<Object> values = entries.values();
//            此时这个map的数值是我们的要找的那个查询的shoptype的集合
//            遍历这个集合的时候
            for (Object c:values) {
                ShopType shopType= JSONUtil.toBean((String) c,ShopType.class);
                list.add(shopType);
            }
//            将其转换为bean进行存储到list中
            list.sort((o1,o2)->{
                if (o1.getSort()>=o2.getSort()){
                    return 1;
                }else {
                    return -1;
                }
            });
//            针对这个list进行排序的操作
//            将排序后的结果返回
            return list;
        }
//        上面的部分是命中缓存的部分
        List<ShopType> sort = query().orderByAsc("sort").list();
//如果米有命中，则继续查询我们的数据库
        if (sort.isEmpty()){
            return null;
        }
        Map<String,Object> map=new HashMap<>();
        for(ShopType shopType:sort){
            map.put(shopType.getId().toString(),JSONUtil.toJsonStr(shopType));
        }
//        不能直接将对象放进去，必须放回的是我们的字符串，需要将此处的查询到的进行转换
        stringRedisTemplate.opsForHash().putAll(RedisConstants.ShopType,map);
        return sort;
    }
}
