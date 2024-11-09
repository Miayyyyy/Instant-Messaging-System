package com.lld.im.service.group.service;


import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.group.model.req.SendGroupMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.message.service.CheckSendMessageService;
import com.lld.im.service.message.service.MessageStoreService;
import com.lld.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupMessageService {
    @Autowired
    CheckSendMessageService checkSendMessageService;
    @Autowired
    MessageProducer messageProducer;
    @Autowired
    ImGroupMemberService imGroupMemberService;
    @Autowired
    MessageStoreService messageStoreService;
    public void process(GroupChatMessageContent messageContent){
        String fromId = messageContent.getFromId();
        String groupId = messageContent.getGroupId();
        Integer appId = messageContent.getAppId();
        //0 前置校验
        //这个用户是否被禁烟，是否被禁用
        //发送方和接收方是否为好友

        ResponseVO responseVO = imServerPermissionCheck(fromId,groupId,appId);
        if(responseVO.isOk()){
            //1 回ack给自己
            ack(messageContent,responseVO);
            //2 发消息给同步端
            syncToSender(messageContent,messageContent);
            //3 发消息给对方在线端
            dispatchMessage(messageContent);

        }else{
            //告诉客户端失败了
            //ack
            ack(messageContent,responseVO);
        }
    }
    private void ack(MessageContent messageContent, ResponseVO responseVO){
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        messageProducer.sendToUser(messageContent.getFromId(), GroupEventCommand.MSG_GROUP,
                responseVO,messageContent);
    }
    private void syncToSender(GroupChatMessageContent messageContent, ClientInfo clientInfo){

        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                MessageCommand.MSG_P2P,messageContent,messageContent);

    }
    private void dispatchMessage(GroupChatMessageContent messageContent){
        List<String> groupMemberId = imGroupMemberService.getGroupMemberId(messageContent.getGroupId(),messageContent.getAppId());
        for(String memberId: groupMemberId){
            if(!memberId.equals(messageContent.getFromId())){
                messageProducer.sendToUser(memberId,GroupEventCommand.MSG_GROUP,
                        messageContent,messageContent.getAppId());
            }
        }


    }
    public ResponseVO imServerPermissionCheck(String fromId, String toId,
                                              Integer appId){
        ResponseVO responseVO = checkSendMessageService.checkGroupMessage(fromId,toId,appId);

        return responseVO;
    }

    public SendMessageResp send(SendGroupMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        BeanUtils.copyProperties(req,message);

        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        //2.发消息给同步在线端
        syncToSender(message,message);
        //3.发消息给对方在线端
        dispatchMessage(message);

        return sendMessageResp;

    }
}
