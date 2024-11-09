package com.lld.im.tcp.server;

import com.lld.im.codec.WebSocketMessageDecoder;
import com.lld.im.codec.WebSocketMessageEncoder;
import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimWebSocketServer {
    private final static Logger logger = LoggerFactory.getLogger(LimWebSocketServer.class);
    BootstrapConfig.TcpConfig config;
    EventLoopGroup mainGroup;
    EventLoopGroup subGroup;
    ServerBootstrap server;
    public LimWebSocketServer(BootstrapConfig.TcpConfig config){
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
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        //http decode and encode
                        channelPipeline.addLast("http-codec",new HttpServerCodec());
                        //big data stream
                        channelPipeline.addLast("http-chunked",new ChunkedWriteHandler());
                        //
                        channelPipeline.addLast("aggregator",new HttpObjectAggregator(65535));
                        /**
                         * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                         * 本handler会帮你处理一些繁重的复杂的事
                         * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                         * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                         */
                        channelPipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                        channelPipeline.addLast(new WebSocketMessageDecoder());
                        channelPipeline.addLast(new WebSocketMessageEncoder());
                        channelPipeline.addLast(new NettyServerHandler(config.getBrokerId(),config.getLogicUrl()));



                    }
                });
        logger.info("Web start");
    }
    public void start(){
        this.server.bind(config.getWebSocketPort());
    }
}
