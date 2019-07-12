package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.entity.CommonConfig;
import com.abc.service.CommonConfigService;
import com.abc.util.PageUtils;
import com.abc.vo.CommonConfigQueryCondition;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@PermInfo(value = "通用配置模块", pval = "a:commonConfig:接口")
@RestController
@RequestMapping("/config/common")
public class CommonConfigController {

    private static final Logger logger = LoggerFactory.getLogger(CommonConfigController.class);

    @Autowired
    private CommonConfigService commonConfigService;

    @PermInfo("添加通用配置")
    @RequiresPermissions("a:config:common:add")
    @PostMapping
    public Json add(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "add common config";
        CommonConfig objInDB = commonConfigService.selectOne(new EntityWrapper<CommonConfig>().eq("name", commonConfigVo.getName()));
        if (objInDB != null) {
            return Json.fail(oper, "该名称已被使用");
        }
        commonConfigVo.setCreated(new Date());
        commonConfigVo.setUpdated(commonConfigVo.getCreated());

        boolean success = commonConfigService.insert(commonConfigVo.toEntity());
        return Json.result(oper, success, commonConfigVo);
    }

    @PermInfo("更新通用配置")
    @RequiresPermissions("a:config:common:update")
    @PutMapping
    public Json update(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "update common config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, JSON.toJSONString(commonConfigVo));
        }

        CommonConfig commonConfigDB = commonConfigService.selectOne(new EntityWrapper<CommonConfig>().eq("name", commonConfigVo.getName()));
        if (commonConfigDB == null) {
            return Json.fail(oper, "变量不存在");
        }
        commonConfigDB.setValue(commonConfigVo.toEntity().getValue());
        commonConfigDB.setDesc(commonConfigVo.getDesc());
        commonConfigDB.setUpdated(new Date());

        boolean success = commonConfigService.updateById(commonConfigDB);
        return Json.result(oper, success, commonConfigDB);
    }

    @PermInfo("删除通用配置")
    @RequiresPermissions("a:config:common:delete")
    @DeleteMapping(value = "/{id}")
    public Json delete(@PathVariable("id") Long id) {
        String oper = "delete common config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, id);
        }

        boolean success = commonConfigService.deleteById(id);
        return Json.result(oper, success);
    }

    @PermInfo("查询所有通用配置")
    @RequiresPermissions("a:config:common:selectPage")
    @PostMapping("/query")
    public Json query(@RequestBody CommonConfigQueryCondition queryCondition) {
        String oper = "query common config";
        if (logger.isInfoEnabled()) {
            logger.info("{}, body: {}", oper, JSON.toJSONString(queryCondition));
        }
        EntityWrapper<CommonConfig> wrapper = new EntityWrapper<>();
        wrapper.eq("type", queryCondition.getType());
        if (StringUtils.isNotBlank(queryCondition.getKey())) {
            wrapper.like(true, "name", queryCondition.getKey());
            wrapper.or().like(true, "desc", queryCondition.getKey());
        }
        Page<CommonConfig> page = commonConfigService.selectPage(PageUtils.getPageParam(queryCondition), wrapper);
        if (!page.getRecords().isEmpty()) {
            page.getRecords().forEach(item -> {
                item = new CommonConfigVo(item);
            });
        }
        return Json.succ(oper).data("page", page);
    }


}
