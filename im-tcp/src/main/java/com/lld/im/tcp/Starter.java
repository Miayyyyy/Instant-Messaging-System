package com.lld.im.tcp;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.reciver.MessageReciver;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.register.RegistryZK;
import com.lld.im.tcp.register.ZKit;
import com.lld.im.tcp.server.LimServer;
import com.lld.im.tcp.server.LimWebSocketServer;

import com.lld.im.tcp.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Starter {
    public static void main(String[] args) {
        if (args.length > 0) {
            start(args[0]);
        }
    }
    private static void start(String path){
        try{
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(path);
            BootstrapConfig bootstrapConfig = yaml.loadAs(inputStream,BootstrapConfig.class);
            new LimServer(bootstrapConfig.getLim()).start();
            new LimWebSocketServer(bootstrapConfig.getLim()).start();

            RedisManager.init(bootstrapConfig);
            MqFactory.init(bootstrapConfig.getLim().getRabbitmq());
            MessageReciver.init(bootstrapConfig.getLim().getBrokerId()+"");
            registerZK(bootstrapConfig);

        }catch (Exception e){
            e.printStackTrace();
            System.exit(500);
        }
    }
    public static void registerZK(BootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(config.getLim().getZkConfig().getZkAddr(),
                config.getLim().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, hostAddress, config.getLim());
        Thread thread = new Thread(registryZK);
        thread.start();

    }
}
