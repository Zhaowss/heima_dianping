package com.hmdp.config;

import com.hmdp.interceptor.Logininterceptor;
import com.hmdp.interceptor.refreshinterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(new Logininterceptor()).excludePathPatterns(
        "user/code",
              "/user/login",
              "/user/code",
              "/blog/hot",
              "/shop/**",
              "/shop-type/**",
              "/voucher/**"
      ).order(1);

//      拦截所有的请求，即每一个请求都可以刷新我们的token的保存的时间
      registry.addInterceptor(new refreshinterceptor(stringRedisTemplate)).order(0);
//     我们设置这个order完成这个我们的interceptor
    }
}
