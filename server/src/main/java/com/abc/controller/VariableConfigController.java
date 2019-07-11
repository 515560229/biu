package com.abc.controller;


import com.abc.annotation.PermInfo;
import com.abc.entity.VariableConfig;
import com.abc.service.VariableConfigService;
import com.abc.vo.Json;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@PermInfo(value = "变量配置模块", pval = "a:config:接口")
@RestController
@RequestMapping("/variableConfig")
public class VariableConfigController {
    private static final Logger logger = LoggerFactory.getLogger(VariableConfigController.class);

    @Autowired
    private VariableConfigService variableConfigService;

    @PermInfo("添加变量")
    @RequiresPermissions("a:config:variable:add")
    @PostMapping
    public Json add(VariableConfig variableConfig) {
        String oper = "add variable config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, JSON.toJSONString(variableConfig));
        }

        VariableConfig variableConfigDB = variableConfigService.selectOne(new EntityWrapper<VariableConfig>().eq("name", variableConfig.getName()));
        if (variableConfigDB != null) {
            return Json.fail(oper, "变量已被使用");
        }
        variableConfig.setCreated(new Date());
        variableConfig.setUpdated(variableConfig.getCreated());

        boolean success = variableConfigService.insert(variableConfig);
        return Json.result(oper, success);
    }

}
