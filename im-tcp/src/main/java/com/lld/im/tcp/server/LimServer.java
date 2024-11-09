package com.lld.im.tcp.server;


import com.lld.im.codec.MessageDecoder;
import com.lld.im.codec.MessageEncoder;
import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.handler.HearBeatHandler;
import com.lld.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimServer {
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);
    BootstrapConfig.TcpConfig config;
    EventLoopGroup mainGroup;
    EventLoopGroup subGroup;
    ServerBootstrap server;

    public LimServer(BootstrapConfig.TcpConfig config){
        this.config = config;
        mainGroup = new NioEventLoopGroup(config.getBossThreadSize());
        subGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        server = new ServerBootstrap();
        server.group(mainGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY,true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new MessageDecoder());
                        //当读写时间超时时，会调用下一个handler中的userEventTriggered方法
//                        socketChannel.pipeline().addLast(new IdleStateHandler(0,0,3));
                        socketChannel.pipeline().addLast(new MessageEncoder());
                        socketChannel.pipeline().addLast(new HearBeatHandler(config.getHeartBeatTime()));
                        socketChannel.pipeline().addLast(new NettyServerHandler(config.getBrokerId(),config.getLogicUrl()));
                    }
                });
    }
    public void start(){
        this.server.bind(config.getTcpPort());
    }
}
