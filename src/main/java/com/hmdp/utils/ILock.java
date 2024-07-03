package com.hmdp.utils;

public interface ILock {

//    尝试获取锁

    boolean trylock(long timeoutSec);
//    取消锁
    void unlock();

}
