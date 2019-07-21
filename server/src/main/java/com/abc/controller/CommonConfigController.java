package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.entity.CommonConfig;
import com.abc.service.CommonConfigService;
import com.abc.util.PageUtils;
import com.abc.vo.CommonConfigQueryCondition;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.Option;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.mysql.jdbc.Driver;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

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
        if (existsByName(commonConfigVo) > 0) {
            return Json.fail(oper, String.format("该名称[%s]已被使用", commonConfigVo.getName()));
        }
        commonConfigVo.setCreated(new Date());
        commonConfigVo.setUpdated(commonConfigVo.getCreated());

        boolean success = commonConfigService.insert(commonConfigVo.toEntity());
        return Json.result(oper, success, commonConfigVo);
    }

    @PermInfo("测试连接")
    @RequiresPermissions("a:config:common:add")
    @PostMapping(value = "/db/testDBConnection")
    public Json testDBConnection(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "testDBConnection";
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setUrl(String.format("jdbc:mysql://%s:%s/%s", commonConfigVo.getDataBaseConfig().getHost(),
                commonConfigVo.getDataBaseConfig().getPort(),
                commonConfigVo.getDataBaseConfig().getDbName()));
        dataSource.setUsername(commonConfigVo.getDataBaseConfig().getUsername());
        dataSource.setPassword(commonConfigVo.getDataBaseConfig().getPassword());
        try {
            dataSource.getConnection();
            return Json.result(oper, true);
        } catch (SQLException e) {
            return Json.result(oper, false).msg(String.format("%s:%s", e.getErrorCode(), e.getMessage()));
        }
    }

    @PermInfo("更新通用配置")
    @RequiresPermissions("a:config:common:update")
    @PutMapping
    public Json update(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "update common config";
        if (logger.isInfoEnabled()) {
            logger.info(oper, JSON.toJSONString(commonConfigVo));
        }

        if (!existsByName(commonConfigVo).equals(commonConfigVo.getId())) {
            return Json.fail(oper, String.format("该名称[%s]已被使用", commonConfigVo.getName()));
        }

        CommonConfig commonConfigDB = commonConfigService.selectById(commonConfigVo.getId());
        if (commonConfigDB == null) {
            return Json.fail(oper, "记录不存在");
        }
        commonConfigDB.setName(commonConfigVo.getName());
        commonConfigDB.setValue(commonConfigVo.toEntity().getValue());
        commonConfigDB.setDesc(commonConfigVo.getDesc());
        commonConfigDB.setUpdated(new Date());

        boolean success = commonConfigService.updateById(commonConfigDB);
        return Json.result(oper, success, commonConfigDB);
    }

    private Long existsByName(CommonConfigVo commonConfigVo) {
        CommonConfig objInDB = commonConfigService.selectOne(
                new EntityWrapper<CommonConfig>()
                        .eq("name", commonConfigVo.getName())
                        .eq("type", commonConfigVo.getType())
        );
        if (objInDB != null) {
            return objInDB.getId();
        }
        return -1L;
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
        wrapper.eq("`type`", queryCondition.getType());
        if (StringUtils.isNotBlank(queryCondition.getKey())) {
            wrapper.andNew().like(true, "`name`", queryCondition.getKey());
            wrapper.or().like(true, "`desc`", queryCondition.getKey());
        }
        Page<CommonConfig> page = commonConfigService.selectPage(PageUtils.getPageParam(queryCondition), wrapper);
        if (!page.getRecords().isEmpty()) {
            for (int i = 0; i < page.getRecords().size(); i++) {
                CommonConfigVo vo = new CommonConfigVo(page.getRecords().get(i));
                page.getRecords().set(i, vo);
            }
        }
        return Json.succ(oper).data("page", page);
    }


}
