package com.abc.vo;

import com.abc.constant.ConfigType;
import com.abc.entity.CommonConfig;
import com.abc.vo.commonconfigvoproperty.DataBaseConfig;
import com.abc.vo.commonconfigvoproperty.DbQueryConfig;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CommonConfigVo extends CommonConfig implements PageVo{
    private int current;
    private int size;

    private DataBaseConfig dataBaseConfig;
    private DbQueryConfig dbQueryConfig;

    public CommonConfigVo(CommonConfig commonConfig) {
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            setDataBaseConfig(JSON.parseObject(commonConfig.getValue(), DataBaseConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.DB_QUERY_CONFIG.getValue())) {
            setDbQueryConfig(JSON.parseObject(commonConfig.getValue(), DbQueryConfig.class));
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
        } else if (commonConfig.getType().equals(ConfigType.DB_QUERY_CONFIG.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getDbQueryConfig()));
        }
        return commonConfig;
    }
}
