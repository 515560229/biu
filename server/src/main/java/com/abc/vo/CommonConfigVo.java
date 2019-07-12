package com.abc.vo;

import com.abc.constant.ConfigType;
import com.abc.entity.CommonConfig;
import com.alibaba.fastjson.JSON;
import org.apache.commons.beanutils.BeanUtils;

public class CommonConfigVo extends CommonConfig{

    private DataBaseConfig dataBaseConfig;

    public DataBaseConfig getDataBaseConfig() {
        return dataBaseConfig;
    }

    public void setDataBaseConfig(DataBaseConfig dataBaseConfig) {
        this.dataBaseConfig = dataBaseConfig;
    }

    public CommonConfigVo(CommonConfig commonConfig) {
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            setDataBaseConfig(JSON.parseObject(getValue(), DataBaseConfig.class));
        }

        this.setValue(null);
    }

    public CommonConfig toEntity() {
        CommonConfig commonConfig = new CommonConfig();
        try {
            BeanUtils.copyProperties(commonConfig, this);
        } catch (Exception e) {
            //do nothing
        }
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            setValue(JSON.toJSONString(getDataBaseConfig()));
        }
        return commonConfig;
    }

    public static class DataBaseConfig  {
        private String host;
        private int port;
        private String dbName;
        private String dbType = "mysql";//default mysql

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDbName() {
            return dbName;
        }

        public void setDbName(String dbName) {
            this.dbName = dbName;
        }

        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }
    }

}
