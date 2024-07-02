package com.hmdp;

import com.hmdp.utils.RedisIdworker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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


}
