package com.lld.im.tcp.register;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.common.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryZK implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(RegistryZK.class);
    private ZKit zKit;
    private String ip;
    private BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig){
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }
    @Override
    public void run() {
        zKit.createRootNode();
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        logger.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        String webPath =
                Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        //todo 不知道是不是他错了
        logger.info("Registry zookeeper webPath success, msg=[{}]", tcpPath);

    }

}
