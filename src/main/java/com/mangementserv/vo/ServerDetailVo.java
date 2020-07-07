package com.mangementserv.vo;

import com.mangementserv.enumaration.StatusEnum;
import lombok.Data;

@Data
public class ServerDetailVo {

    private String name;
    private StatusEnum status;


}
