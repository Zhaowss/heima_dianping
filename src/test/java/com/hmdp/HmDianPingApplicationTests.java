package com.hmdp;

import com.hmdp.utils.RedisIdworker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {


    @Autowired
    private RedisIdworker redisIdworker;

    private ExecutorService es= Executors.newFixedThreadPool(
        2
    );
    @Test
    void testredis(){
        Runnable task=()->{
            for (int i = 0; i < 100; i++) {
                long l = redisIdworker.nextId("order");
                System.out.println(l);
            }
//            System.out.println("hello");
        };
        for (int j = 0; j < 100; j++) {
            es.submit(task);
        }

    }
    @Autowired
    private  RedissonClient redissonClient;

    @Test
    void testRedisson() throws InterruptedException {
        //获取锁(可重入)，指定锁的名称
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1,10, TimeUnit.SECONDS);
        //判断获取锁成功
        if(isLock){
            try{
                System.out.println("执行业务");
            }finally{
                //释放锁
                System.out.println("释放锁");
                lock.unlock();
            }

        }


    }


}
