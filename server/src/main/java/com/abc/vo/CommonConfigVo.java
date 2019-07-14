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

    public CommonConfigVo() {
        //do nothing
    }

    public CommonConfigVo(CommonConfig commonConfig) {
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            setDataBaseConfig(JSON.parseObject(commonConfig.getValue(), DataBaseConfig.class));
        }
        try {
            BeanUtils.copyProperties(this, commonConfig);
        } catch (Exception e) {
            //do nothing
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
            commonConfig.setValue(JSON.toJSONString(getDataBaseConfig()));
        }
        return commonConfig;
    }
}
