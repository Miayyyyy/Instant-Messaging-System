package com.lld.im.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.tcp.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import jodd.util.StringUtil;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {
    private static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();

    public static void put(Integer appId, String userId,Integer clientType,String imei, NioSocketChannel channel){
        UserClientDto dto = new UserClientDto();
        dto.setImei(imei);
        dto.setAppId(appId);
        dto.setClientType(clientType);
        dto.setUserId(userId);
        CHANNELS.put(dto, channel);
    }

    public static NioSocketChannel get(Integer appId, String userId,Integer clientType,String imei){
        UserClientDto dto = new UserClientDto();
        dto.setAppId(appId);
        dto.setClientType(clientType);
        dto.setUserId(userId);
        dto.setImei(imei);
        return CHANNELS.get(dto);
    }
    public static List<NioSocketChannel> get(Integer appId , String id) {

        Set<UserClientDto> channelInfos = CHANNELS.keySet();
        List<NioSocketChannel> channels = new ArrayList<>();

        channelInfos.forEach(channel ->{
            if(channel.getAppId().equals(appId) && id.equals(channel.getUserId())){
                channels.add(CHANNELS.get(channel));
            }
        });

        return channels;
    }

    public static void remove(Integer appId, String userId,Integer clientType,String imei){
        UserClientDto dto = new UserClientDto();
        dto.setAppId(appId);
        dto.setClientType(clientType);
        dto.setUserId(userId);
        dto.setImei(imei);
        CHANNELS.remove(dto);
    }

    public static void remove(NioSocketChannel channel){
        CHANNELS.entrySet().stream().filter(entity-> entity.getValue() == channel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }
    public static void removeUserSession(NioSocketChannel nioSocketChannel){
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String)nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId,userId,clientType,imei);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<Object,Object> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType+":"+imei);
        nioSocketChannel.close();
    }
    public static void offlineUserSession(NioSocketChannel nioSocketChannel){
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String)nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();


        SessionSocketHolder.remove(appId,userId,clientType,imei);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String ,String > map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        String sessionStr = map.get(clientType.toString());

        if(!StringUtil.isBlank(sessionStr)){
            UserSession userSession = JSONObject.parseObject(sessionStr,UserSession.class);
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType.toString()+":"+imei, JSONObject.toJSONString(userSession));

        }

        nioSocketChannel.close();

    }

}
