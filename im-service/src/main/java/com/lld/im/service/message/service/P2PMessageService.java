package com.lld.im.service.message.service;


import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.message.model.req.SendMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class P2PMessageService {
    private static Logger logger = LoggerFactory.getLogger(P2PMessageService.class);
    @Autowired
    CheckSendMessageService checkSendMessageService;
    @Autowired
    MessageProducer messageProducer;

    private final ThreadPoolExecutor threadPoolExecutor;
    {
        final AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("message-process-thread-" + num.getAndIncrement());
                return thread;
            }
        });
    }

    @Autowired
    MessageStoreService messageStoreService;

    public P2PMessageService() {
    }

    public void process(MessageContent messageContent){
        String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();
        //0 前置校验
        //这个用户是否被禁烟，是否被禁用
        //发送方和接收方是否为好友

        ResponseVO responseVO = imServerPermissionCheck(fromId,toId,messageContent.getAppId());
        if(responseVO.isOk()){
            threadPoolExecutor.execute(()->{
                messageStoreService.storeP2PMessage(messageContent);
                //
                //1 回ack给自己
                ack(messageContent,responseVO);
                //2 发消息给同步端
                syncToSender(messageContent,messageContent);
                //3 发消息给对方在线端
                dispatchMessage(messageContent);
            });


        }else{
            //告诉客户端失败了
            //ack
            ack(messageContent,responseVO);
        }
    }
    private void ack(MessageContent messageContent, ResponseVO responseVO){
        logger.info("msg ack, msgId={},checkResult{}",messageContent.getMessageId(),responseVO.getCode());
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK,
                responseVO,messageContent);
    }
    private void syncToSender(MessageContent messageContent, ClientInfo clientInfo){

        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                MessageCommand.MSG_P2P,messageContent,messageContent);

    }
    private void dispatchMessage(MessageContent messageContent){
        messageProducer.sendToUser(messageContent.getToId(),MessageCommand.MSG_P2P,
                messageContent,messageContent.getAppId());

    }
    public ResponseVO imServerPermissionCheck(String fromId,String toId,
                                              Integer appId){
        ResponseVO responseVO = checkSendMessageService.checkSenderForvidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }

    public SendMessageResp send(SendMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        BeanUtils.copyProperties(req,message);
        //插入数据
        messageStoreService.storeP2PMessage(message);
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());

        //2.发消息给同步在线端
        syncToSender(message,message);
        //3.发消息给对方在线端
        dispatchMessage(message);
        return sendMessageResp;
    }

}
