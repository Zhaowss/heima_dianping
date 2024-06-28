package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jws.soap.SOAPBinding;
import javax.management.MBeanAttributeInfo;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private UserMapper userMapper;


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
//        校验手机号,正则表达实现验证
          if (!RegexUtils.isCodeInvalid(phone)) {
              return Result.fail("手机号格式错误");
          }

//        如果不符合的时候 我们返回错误的信息

//        生成我们的验证码
        String code= RandomUtil.randomNumbers(6);
//        报存验证码，验证码保存在我们redis中
//        session.setAttribute("code",code);
//        我们需要对这个key的结果进行一个设置,此外这里的手机号需要token的加密初始化。
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        发送验证码
        log.debug("发送短信验证码成功"+code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        校验手机号合验证码
        String phone = loginForm.getPhone();
        String code =loginForm.getCode();
        if (!RegexUtils.isCodeInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

//        校验验证码
        if (code==null  || !stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone).equals(code)){
            return Result.fail("验证码错误");
        }

//       查询一致的时候进行手机号查询用户

        User user = query().eq("phone", phone).one();
        if (user==null){
            user = CreateUserWithPhone(phone);
        }

//        存入的session的信息的时候需要传入的是我们用户的所有的信息，导致负载过重
//        这个我们就需要进行保存的存储。
//        TODO 1。生成一个token的key
//        随机生成一个登陆令牌进行登陆的验证的操作
        String token=UUID.randomUUID().toString(true);
//        TODO 2.将我们的user转为hash的存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//      将我们的

//        TODO  3.存储的操作
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreCase(true).setFieldValueEditor(
                (name,value)->value.toString()));

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY+token,stringObjectMap);
//        设置用户的有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
//      token的前段的返回的操作

        return Result.ok(token);
    }

    private User CreateUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
        return  user;
    }
}
