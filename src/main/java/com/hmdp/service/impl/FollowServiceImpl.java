package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.ws.Holder;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Autowired
     private  StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IUserService userService;
    @Override
    public Result getOrnot(Long id) {
        Long userid = UserHolder.getUser().getId();
//        select * from tb_follow where  user_id=userid and follow_id=id
        Integer count = query().eq("user_id", userid).eq("follow_user_id", id).count();
        return Result.ok(count>0);
    }

    @Override
    public Result isfollow(Long id, Boolean isfollow) {
//        获取当前userid
        Long id1 = UserHolder.getUser().getId();
        String key="follow:"+id1;
//        获取当前的要关注的用户ID
//        如果是关注
        if (isfollow){
            Follow follow = new Follow();
            follow.setUserId(id1);
            follow.setFollowUserId(id);
            boolean save = save(follow);
            if (save){
                stringRedisTemplate.opsForSet().add(key,id.toString());
            }
        }else {
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", id1).eq("follow_user_id", id));
            if (remove){
                stringRedisTemplate.opsForSet().remove(key,id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result common(Long id) {
        Long userid = UserHolder.getUser().getId();
        String key="follow:"+userid;
        String key1="follow:"+id;


//        其中ID为当前的查看用户的一个id标识
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key);

//        其中userid为当前的操作用户的ID

        if (intersect==null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> collect = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> collect1 = userService.listByIds(collect).stream().map(user ->
                BeanUtil.copyProperties(user, UserDTO.class)
        ).collect(Collectors.toList());
        return Result.ok(collect1);
    }
}
