package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.service.impl.FollowServiceImpl;
import com.hmdp.utils.UserHolder;
import org.jcp.xml.dsig.internal.dom.ApacheCanonicalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {


    @Autowired
    private IFollowService followService;
    @GetMapping("/or/not/{id}")
    public Result getOrnot(@PathVariable("id")Long id){
//        查询判断是否已经点赞
        return   followService.getOrnot(id);
    }


    @PutMapping("{id}/{isFollow}")
    public  Result follow(@PathVariable("id")Long id, @PathVariable("isFollow")Boolean isfollow){
//        关注点赞和取消点赞的接口
        return  followService.isfollow(id,isfollow);
    }

    @GetMapping("/common/{id}")
    public  Result commonFollow(@PathVariable("id")Long id){

        return followService.common(id);

    }


}
