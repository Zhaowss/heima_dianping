package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdworker;
import com.hmdp.utils.UserHolder;
import com.sun.xml.internal.fastinfoset.vocab.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {


    @Autowired
    private SeckillVoucherServiceImpl seckillVoucherService;

    @Autowired
    private RedisIdworker redisIdworker;

    @Override
    @Transactional
    public Result seckill(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        if (LocalDateTime.now().isBefore(beginTime)){
            return Result.fail("秒杀尚未开始");
        }

        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (LocalDateTime.now().isAfter(endTime)){
            return Result.fail("秒杀已经结束");
        }

        if (seckillVoucher.getStock()<1){
            return Result.fail("库存不足");
        }
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).update();
        if (!success){
            return Result.fail("库存不足");
        }

        VoucherOrder voucherOrder=new VoucherOrder();

//        shengcheng 全局唯一ID

        long orderid = redisIdworker.nextId("order");
        voucherOrder.setId(orderid);
        Long id = UserHolder.getUser().getId();
        voucherOrder.setUserId(id);
//        代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(voucherId);
    }
}
