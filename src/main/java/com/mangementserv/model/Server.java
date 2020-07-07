package com.mangementserv.model;

import com.mangementserv.enumaration.CommandEnum;
import lombok.Data;

import java.util.Map;

@Data
public class Server {

    private String name;

    private String snapShotName;

    private Map<CommandEnum,String> commands;
    
}
