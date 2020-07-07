package com.mangementserv.vo;

import com.mangementserv.enumaration.CommandEnum;
import lombok.Data;

@Data
public class ServerActionVo {

    private String name;

    private CommandEnum action;

}
