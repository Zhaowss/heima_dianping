package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class refreshinterceptor implements  HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;
    public refreshinterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        TODO  获取我们的请求中的一些认证的token
        String token = request.getHeader("authorization");
        if (token==null){
            return  true;
        }
//        基token的用户的信息的查询
        Map<Object, Object> entries = this.stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);

        if (entries.isEmpty()){
            return true;
        }

//        将用户有map转为bean的对象
        UserDTO userdto = (UserDTO)BeanUtil.fillBeanWithMap(entries,new UserDTO(),false);
//        判断用户是否存在，判断我们的当前的用户的信息是否是存在的

         UserHolder.saveUser(userdto);
//        刷新我们的用户的token的有效期
        this.stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
