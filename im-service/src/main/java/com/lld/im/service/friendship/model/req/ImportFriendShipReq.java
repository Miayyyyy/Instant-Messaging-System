package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ImportFriendShipReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    public String fromId;

    public List friendItem;

    @Data
    public static class ImportFriendDto{
        private String toId;

        private String remark;

        private String addSource;

        private Integer status;

        private Integer black;
    }
}
