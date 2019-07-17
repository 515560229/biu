package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

@Data
public class DataBaseConfig {
    private String host;
    private int port;
    private String dbName;
    private String dbType = "mysql";//default mysql
    private String username;
    private String password;
}