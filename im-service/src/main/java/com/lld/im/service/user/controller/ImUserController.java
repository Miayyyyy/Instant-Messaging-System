package com.lld.im.service.user.controller;

import com.lld.im.common.ClientType;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.route.RouteHandle;
import com.lld.im.common.route.RouteInfo;
import com.lld.im.common.utils.RouteInfoParseUtil;
import com.lld.im.service.user.model.req.DeleteUserReq;
import com.lld.im.service.user.model.req.ImportUserReq;
import com.lld.im.service.user.model.req.LoginReq;
import com.lld.im.service.user.service.ImUserService;
import com.lld.im.service.utils.ZKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Autowired
    ImUserService imUserService;

    @Autowired
    RouteHandle routeHandle;

    @Autowired
    ZKit zKit;

    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.importUser(req);
    }
    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    /**
     * im的登录接口，返回im地址
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);
        if(login.isOk()){
            //todo 去zk获取一个im的地址，返回给sdk
            List<String> allNode = new ArrayList<>();
            if(req.getClientType() == ClientType.WEB.getCode()){
                allNode = zKit.getAllWebNode();
            }else{
                allNode = zKit.getAllTcpNode();
            }
            String s = routeHandle.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

}
