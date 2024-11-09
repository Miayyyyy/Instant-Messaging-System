package com.lld.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.codec.pack.LoginPack;
import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.enums.command.SystemCommand;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.common.model.message.CheckSendMessageReq;
import com.lld.im.tcp.feign.FeignMessageService;
import com.lld.im.tcp.publish.MqMessageProducer;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.InetAddress;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
    private Integer brokerId;

    private String logicUrl;

    private FeignMessageService feignMessageService;
    public NettyServerHandler(Integer brokerId, String logicUrl){
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))//设置超时时间
                .target(FeignMessageService.class, logicUrl);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
//        System.out.println(message);
        //
        Integer command = message.getMessageHeader().getCommand();
        //login command
        if(command == SystemCommand.LOGIN.getCommand()){
            //拿到login pack 为channel设置属性
            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()),
                    new TypeReference<LoginPack>(){}.getType());

            //为channel设置自定义属性用户id
            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
            ctx.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());
            //store channel

            //Redis map
            UserSession userSession = new UserSession();
            userSession.setAppId(message.getMessageHeader().getAppId());
            userSession.setClientType(message.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setBrokerId(brokerId);
            userSession.setImei(message.getMessageHeader().getImei());
            try{
                InetAddress locahHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(locahHost.getHostAddress());
            }catch (Exception e){
                e.printStackTrace();
            }
            //todo 存到redis
            RedissonClient redissonClient = RedisManager.getRedissonClient();

            //得到redis中key-value对中的value，该key为自定义的 appid + constants + userId, value为RMap， 如果getMap()参数中的key不存在，会先创建再返回
            RMap<String, String> map = redissonClient.getMap(message.getMessageHeader().getAppId()+ Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            //得到了某个用户的session，然后将对应的key-value存进入，key：client type， value：userSession
            map.put(message.getMessageHeader().getClientType()+":"+message.getMessageHeader().getImei(),JSONObject.toJSONString(userSession));

            SessionSocketHolder.put(message.getMessageHeader().getAppId(),loginPack.getUserId(),message.getMessageHeader().getClientType(),message.getMessageHeader().getImei(),(NioSocketChannel)ctx.channel());

            UserClientDto dto = new UserClientDto();
            dto.setImei(message.getMessageHeader().getImei());
            dto.setUserId(loginPack.getUserId());
            dto.setClientType(message.getMessageHeader().getClientType());
            dto.setAppId(message.getMessageHeader().getAppId());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(dto));

        }else if(command == SystemCommand.LOGOUT.getCommand()){
            //todo
            //delete session
            //redis delete
            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());
        }else if(command == SystemCommand.PING.getCommand()){
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
        }else if(command == MessageCommand.MSG_P2P.getCommand()){
            CheckSendMessageReq req = new CheckSendMessageReq();
            req.setAppId(message.getMessageHeader().getAppId());
            req.setCommand(message.getMessageHeader().getCommand());
            JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()));
            String fromId = jsonObject.getString("fromId");
            String toId = jsonObject.getString("toId");
            req.setToId(toId);
            req.setFromId(fromId);
            //todo 1调用校验发送方的接口
            ResponseVO responseVO = feignMessageService.checkSendMessage(req);
            if(responseVO.isOk()){
                MqMessageProducer.sendMessage(message,command);
            }else{
                //todo ack
                ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                responseVO.setData(chatMessageAck);
                MessagePack<ResponseVO> ack = new MessagePack<>();
                ack.setData(responseVO);
                ack.setCommand(MessageCommand.MSG_ACK.getCommand());
                ctx.channel().writeAndFlush(ack);
            }

            //如果成功投递到mq
            //失败则直接ack

        }
        else{
            //把消息发送给逻辑层
            MqMessageProducer.sendMessage(message,command);
        }

    }

}
