package com.hmdp.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

public class SimleRedisLock implements ILock{

    private String name;
    private static  final  String KEY_PREFIX="Lock:";

    private StringRedisTemplate stringRedisTemplate;

    public SimleRedisLock(String s,StringRedisTemplate stringRedisTemplate) {
        this.name=s;
        this.stringRedisTemplate=stringRedisTemplate;
    }

    @Override
    public boolean trylock(long timeoutSec) {
        long id = Thread.currentThread().getId();
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }

    @Override
    public void unlock() {
        Boolean delete = stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
