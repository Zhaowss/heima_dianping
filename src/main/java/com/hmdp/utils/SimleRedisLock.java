package com.hmdp.utils;


import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimleRedisLock implements ILock{

    private String name;
    private static  final  String KEY_PREFIX="Lock:";
    private static  final  String ID_PREFIX= UUID.randomUUID().toString(true)+"-";

    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation((Resource) new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimleRedisLock(String s,StringRedisTemplate stringRedisTemplate) {
        this.name=s;
        this.stringRedisTemplate=stringRedisTemplate;
    }


    @Override
    public boolean trylock(long timeoutSec) {
        String id = ID_PREFIX+Thread.currentThread().getId();
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }

    @Override
    public void unlock() {
//     通过获取我们锁中的用户的设备UUID+当前线程的唯一ID，保证每个单体服务器上的用户ID的唯一性，避免出现ID判断冲突，出现锁的误删
//        String id = ID_PREFIX+Thread.currentThread().getId();
//        String s_id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
////    判断当前的线程的ID和我们锁中的ID的是否一致，如果一致，则释放锁，否则不做任何操作。
//        if (id.equals(s_id)){
//            Boolean delete = stringRedisTemplate.delete(KEY_PREFIX + name);
//        }

//       基于LUA脚本的具体的实现
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
