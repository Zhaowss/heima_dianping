package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdworker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {


    @Autowired
    private SeckillVoucherServiceImpl seckillVoucherService;

    @Autowired
    private RedisIdworker redisIdworker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;


   private IVoucherOrderService proxy;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("streamseckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    private BlockingQueue<VoucherOrder> orderstsks=new ArrayBlockingQueue<VoucherOrder>(1024*1024);

    private static final ExecutorService seckill_order_executor= Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        seckill_order_executor.submit(new VoucherOrderHandler());
    }
//    private class  VoucherOrderHandler implements Runnable{
//        @Override
//        public void run() {
//            while (true){
////                获取队列中的信息
//                try {
//                    VoucherOrder take = orderstsks.take();
//                    //                创建订单
//                   handleseckillvoucher(take);
//                } catch (Exception e) {
//                   log.error("订单异常",e);
//                }
//
//
//            }
//
//        }
//
//
//    }


//    实现我们以消费者组的形式获取我们的消息
    private class  VoucherOrderHandler implements Runnable {
    String queueName = "stream.orders";

    @Override
    public void run() {
        while (true) {
//                获取队列中的信息
            try {
//                    获取我们的消息队列中的信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                List<MapRecord<String, Object, Object>> mapRecordList = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(queueName, ReadOffset.lastConsumed())
                );

//                    判断消息的获取是否成功

                if (mapRecordList == null || mapRecordList.isEmpty()) {
                    continue;
                }
//                    解析消息中的订单信息
                MapRecord<String, Object, Object> entries =
                        mapRecordList.get(0);
//                    如果获取失败则说明没有消息 进入下次循坏
                Map<Object, Object> values = entries.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
//                    获取成功 则可以下单
                handleseckillvoucher(voucherOrder);
//                    下单成功之后 ACK确认
                stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());
            } catch (Exception e) {
                haddlepeddingstreamlist();
                log.error("订单异常", e);
            }


        }

    }

    private void haddlepeddingstreamlist() {
        while (true) {
//                获取队列中的信息
            try {
//                    获取我们的消息队列中的信息 XREADGROUP GROUP g1 c1 COUNT 1  STREAMS streams.order 0
                List<MapRecord<String, Object, Object>> mapRecordList = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(queueName, ReadOffset.from(String.valueOf(0)))
                );

//                    判断消息的获取是否成功

                if (mapRecordList == null || mapRecordList.isEmpty()) {
                    break;
                }
//                    解析消息中的订单信息
                MapRecord<String, Object, Object> entries =
                        mapRecordList.get(0);
//                    如果获取失败则说明没有消息 进入下次循坏
                Map<Object, Object> values = entries.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
//                    获取成功 则可以下单
                handleseckillvoucher(voucherOrder);
//                    下单成功之后 ACK确认
                stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());
            } catch (Exception e) {
                log.error("处理消息订单异常", e);
            }

        }
    }

    private void handleseckillvoucher(VoucherOrder take) {
        Long userId = take.getUserId();
        RLock trylock = redissonClient.getLock("order:" + userId);
        boolean lock = trylock.tryLock();
        if (!lock) {
            log.error("不允许下单");
        }
        try {
            proxy.getResult(take);
        } finally {
            trylock.unlock();
        }

    }
}



    @Override
    public Result seckill(Long voucherId) {
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        LocalDateTime beginTime = seckillVoucher.getBeginTime();
//        if (LocalDateTime.now().isBefore(beginTime)) {
//            return Result.fail("秒杀尚未开始");
//        }
//
//        LocalDateTime endTime = seckillVoucher.getEndTime();
//        if (LocalDateTime.now().isAfter(endTime)) {
//            return Result.fail("秒杀已经结束");
//        }
//
//        if (seckillVoucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//        Long id = UserHolder.getUser().getId();
//      原始的悲观锁的实现，局限于单机的系统
//        synchronized (id.toString().intern()){
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.getResult(voucherId);
//        }
//        分布式锁的自己实现
//        SimleRedisLock simleRedisLock = new SimleRedisLock("order:" + id, stringRedisTemplate);
//        boolean trylock = simleRedisLock.trylock(1200);
//        if (!trylock){
//            return Result.fail("不允许我们重复下单");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.getResult(voucherId);
//        }finally {
//
//            simleRedisLock.unlock();
//        }

//        之间的实现方法,方法可能阻塞导致出现处理速度过慢
//        RLock trylock = redissonClient.getLock("order:" + id);
//        boolean lock = trylock.tryLock();
//        if (!lock) {
//            return Result.fail("不允许我们重复下单");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.getResult(voucherId);
//        } finally {
//            trylock.unlock();
//        }


//      优化这个操作的流程，即判断的部分我们全部放入lua的脚本中利用原子性进行redis中缓存的快速判断
//        Long userid = UserHolder.getUser().getId();
//        Long orderid = redisIdworker.nextId("order");
//
//        Long execute = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
//                voucherId.toString(),
//                userid.toString(),
//                String.valueOf(orderid)
//        );
//        int i = execute.intValue();
//        if (i!=0){
//            return Result.fail(i==1?"库存不足":"不能重复下单");
//        }
//
//        VoucherOrder voucherOrder=new VoucherOrder();
//
//        voucherOrder.setId(orderid);
//
//        voucherOrder.setUserId(userid);
////        代金券id
//        voucherOrder.setVoucherId(voucherId);
//        orderstsks.add(voucherOrder);
//        proxy = (IVoucherOrderService) AopContext.currentProxy();


//        基于stream实现的消息队列的业务

        Long userid = UserHolder.getUser().getId();
        Long orderid = redisIdworker.nextId("order");

        Long execute = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId.toString(),
                userid.toString(),
                String.valueOf(orderid)
        );
        int i = execute.intValue();
        if (i!=0){
            return Result.fail(i==1?"库存不足":"不能重复下单");
        }
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderid);
    }


    @Transactional
    public void getResult(VoucherOrder voucherOrder) {
            Long id = voucherOrder.getUserId();
            Long voucherId=voucherOrder.getVoucherId();
//          实现秒杀一人一单的功能
            Integer count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
            if (count>0){
                 log.error("用户已经购买过了");
            }

            boolean success = seckillVoucherService.update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId).gt("stock",0).update();
            if (!success){
                log.error("库存不足");
            }
            save(voucherOrder);
    }
}
