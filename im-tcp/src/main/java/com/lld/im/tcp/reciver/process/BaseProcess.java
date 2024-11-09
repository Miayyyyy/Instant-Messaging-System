package com.lld.im.tcp.reciver.process;

import com.lld.im.codec.proto.MessagePack;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class BaseProcess {
    //留给日后扩展使用
    public abstract void processBefore();
    public void process(MessagePack messagePack){
        NioSocketChannel nioSocketChannel = SessionSocketHolder.get(messagePack.getAppId(),
                messagePack.getToId(),messagePack.getClientType(),
                messagePack.getImei());
        if(nioSocketChannel != null) {
            nioSocketChannel.writeAndFlush(messagePack);
        }
    }
    public abstract void processAfter();
}
