package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpSession;
import java.util.Random;

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
    @Override
    public Result sendCode(String phone, HttpSession session) {
//        校验手机号,正则表达实现验证
          if (!RegexUtils.isCodeInvalid(phone)) {
              return Result.fail("手机号格式错误");
          }

//        如果不符合的时候 我们返回错误的信息

//        生成我们的验证码
        String code= RandomUtil.randomNumbers(6);
//        报存验证码
        session.setAttribute("code",code);
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
        if (code==null  || !session.getAttribute("code").equals(session.getAttribute("code"))){
            return Result.fail("验证码错误");
        }

//       查询一致的时候进行手机号查询用户

        User user = query().eq("phone", phone).one();
        if (user==null){
            user = CreateUserWithPhone(phone);
        }
        session.setAttribute("user",user);
        return Result.ok();
    }

    private User CreateUserWithPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
        return  user;
    }
}
