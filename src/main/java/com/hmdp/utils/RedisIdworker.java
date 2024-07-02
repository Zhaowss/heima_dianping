package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @ClassNAME RedisIdworker
 * @Description 全局唯一ID的实现
 * @Author zhaoweishan
 * @Date 2024/7/2 06:48
 * @Version 1.0
 */
@Component
public class RedisIdworker {
//    开始的时间戳
    private static final long BEGIN_TIMESTAMP=1640995200L;

    private StringRedisTemplate stringRedisTemplate;

    private int count_bits=32;
    public RedisIdworker(StringRedisTemplate stringRedisTemplate1) {
        this.stringRedisTemplate = stringRedisTemplate1;
    }

    public long nextId(String keyprefix){
//        生成时间戳
        long epochSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestemp=epochSecond-BEGIN_TIMESTAMP;
//        生成序列号
//        获取当前的日期
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("incr:" + keyprefix + ":" + day);
//        拼接并返回

         return timestemp << count_bits | increment;
    }
}
