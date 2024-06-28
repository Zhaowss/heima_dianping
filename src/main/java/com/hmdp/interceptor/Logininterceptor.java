package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.http.HTTPBinding;

public class Logininterceptor implements  HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        HttpSession session = request.getSession();
//        request.getCookies();

        //获取session中的用户

        UserDTO userdto= (UserDTO) session.getAttribute("user");

//        判断用户是否存在
        if (userdto==null) {
//            拦截
            response.setStatus(401);
            return false;
        }
//        不存在直接拦截

        UserHolder.saveUser(userdto);

//        存在则存在threlocal 中

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
