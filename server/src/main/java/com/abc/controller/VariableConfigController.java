package com.abc.controller;


import com.abc.annotation.PermInfo;
import com.abc.entity.VariableConfig;
import com.abc.service.VariableConfigService;
import com.abc.util.CurrentUser;
import com.abc.util.PageUtils;
import com.abc.vo.Json;
import com.abc.vo.VariableQueryConditionVo;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@PermInfo(value = "变量配置模块", pval = "a:variable:接口")
@RestController
@RequestMapping("/config/variable")
public class VariableConfigController {
    private static final Logger logger = LoggerFactory.getLogger(VariableConfigController.class);

    @Autowired
    private VariableConfigService variableConfigService;

    @PermInfo("添加变量")
    @RequiresPermissions("a:variable:add")
    @PostMapping
    public Json add(@RequestBody VariableConfig variableConfig) {
        String oper = "add variable config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, JSON.toJSONString(variableConfig));
        }

        VariableConfig variableConfigDB = variableConfigService.selectOne(new EntityWrapper<VariableConfig>().eq("name", variableConfig.getName()));
        if (variableConfigDB != null) {
            return Json.fail(oper, "变量已被使用");
        }
        variableConfig.setCreated(new Date());
        variableConfig.setCreator(CurrentUser.getUsername());
        variableConfig.setUpdated(variableConfig.getCreated());
        variableConfig.setModifier(CurrentUser.getUsername());

        boolean success = variableConfigService.insert(variableConfig);
        return Json.result(oper, success, variableConfig);
    }

    @PermInfo("更新变量")
    @RequiresPermissions("a:variable:update")
    @PutMapping
    public Json update(@RequestBody VariableConfig variableConfig) {
        String oper = "update variable config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, JSON.toJSONString(variableConfig));
        }

        VariableConfig variableConfigDB = variableConfigService.selectOne(new EntityWrapper<VariableConfig>().eq("name", variableConfig.getName()));
        if (variableConfigDB == null) {
            return Json.fail(oper, "变量不存在");
        }
        variableConfigDB.setValue(variableConfig.getValue());
        variableConfigDB.setDesc(variableConfig.getDesc());
        variableConfigDB.setUpdated(new Date());
        variableConfigDB.setModifier(CurrentUser.getUsername());

        boolean success = variableConfigService.updateById(variableConfigDB);
        return Json.result(oper, success, variableConfigDB);
    }

    @PermInfo("查询变量")
    @RequiresPermissions("a:variable:selectPage")
    @PostMapping("/query")
    public Json query(@RequestBody VariableQueryConditionVo queryCondition) {
        String oper = "query variable";
        if (logger.isInfoEnabled()) {
            logger.info("{}, body: {}", oper, JSON.toJSONString(queryCondition));
        }
        EntityWrapper<VariableConfig> wrapper = new EntityWrapper<>();
        if (queryCondition.getName() != null) {
            wrapper.like("`name`", queryCondition.getName());
        }
        if (queryCondition.getDesc() != null) {
            wrapper.like(true,"`desc`", queryCondition.getDesc());
        }
        if (queryCondition.getValue() != null) {
            wrapper.like(true,"`value`", queryCondition.getValue());
        }
        Page<VariableConfig> page = variableConfigService.selectPage(PageUtils.getPageParam(queryCondition), wrapper);
        return Json.succ(oper).data("page", page);
    }

}
