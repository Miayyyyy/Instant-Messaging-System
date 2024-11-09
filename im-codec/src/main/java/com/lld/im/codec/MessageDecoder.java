package com.lld.im.codec;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        //header: command, version, clientType, decodeType, imei-len, appId, body-len;
        // header + imei + body
        if(in.readableBytes() < 28){
            return;
        }
        int command = in.readInt();
        int version = in.readInt();
        int clientType = in.readInt();
        int messageType = in.readInt();
        int appId = in.readInt();
        int imeiLength = in.readInt();
        int bodyLen = in.readInt();

        if(in.readableBytes() < bodyLen + imeiLength){
            in.resetReaderIndex();
            return;
        }

        byte[] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        byte[] bodyData = new byte[bodyLen];
        in.readBytes(bodyData);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageType(messageType);
        messageHeader.setCommand(command);
        messageHeader.setVersion(version);
        messageHeader.setAppId(appId);
        messageHeader.setClientType(clientType);
        messageHeader.setImeiLength(imeiLength);
        messageHeader.setBodyLen(bodyLen);
        messageHeader.setImei(imei);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if(messageType == 0x0){
            String body = new String(bodyData);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        in.markReaderIndex();
        out.add(message);
    }
}
