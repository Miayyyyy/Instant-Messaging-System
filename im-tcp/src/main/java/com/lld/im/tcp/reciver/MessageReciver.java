package com.lld.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.reciver.process.BaseProcess;
import com.lld.im.tcp.reciver.process.ProcessFactory;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class MessageReciver {
    private static String brokerId;
    public static void startReciverMessage(){
        try{
            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im + brokerId,true,false,false,null);
            //开始阶段，加上此句将无法接收消息 todo
            channel.queueBind(Constants.RabbitConstants.MessageService2Im + brokerId,Constants.RabbitConstants.MessageService2Im,brokerId);
            channel.basicConsume(Constants.RabbitConstants.MessageService2Im + brokerId,false,new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // 处理消息服务发来的消息
                    try{
                        String msgStr = new String(body);
                        MessagePack messagePack = JSONObject.parseObject(msgStr, MessagePack.class);
                        BaseProcess messageProcess = ProcessFactory.getMessageProcess(messagePack.getCommand());
                        messageProcess.process(messagePack);
                        log.info(msgStr);
                        channel.basicAck(envelope.getDeliveryTag(),false);
                    }catch (Exception e){
                        e.printStackTrace();
                        channel.basicNack(envelope.getDeliveryTag(),false,false);
                    }
                }
            });
        }catch (Exception e){

        }
    }
    public static void init(){
        startReciverMessage();
    }
    public static void init(String brokerId){
        if(StringUtils.isBlank(MessageReciver.brokerId)){
            MessageReciver.brokerId = brokerId;
        }
        startReciverMessage();
    }
}
