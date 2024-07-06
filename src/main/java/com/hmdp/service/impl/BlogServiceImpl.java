package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {


    @Autowired
    private  StringRedisTemplate stringRedisTemplate;

    @Autowired
    private  UserServiceImpl userService;
    @Override
    public Result likeblog(Long id) {


        Long userid = UserHolder.getUser().getId();
        String key="blog:like:"+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userid.toString());
        if (score==null){
            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();
            if (isSuccess){
                stringRedisTemplate.opsForZSet().add(key,userid.toString(),System.currentTimeMillis());
            }
        }else {
            boolean isSuccess = update().setSql("liked=liked-1").eq("id", id).update();
            if (isSuccess){
                stringRedisTemplate.opsForZSet().remove(key,userid.toString());
            }
        }
        return  Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {

       String key="blog:like:"+id;
       Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids= top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }


}
