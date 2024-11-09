package com.lld.im.codec.proto;

import lombok.Data;

@Data
public class MessageHeader {
    private Integer command;
    private Integer version;
    private Integer clientType;

    //数据解析类型，和具体业务无关，后续根据解析类型解析data数据
    //0x0:json， 0x1:protoBuf, 0x2: xml
    private Integer messageType = 0x0;
    private Integer appId ;
    private Integer imeiLength ;
    private Integer bodyLen ;
    private String imei;
}
