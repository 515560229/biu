package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.entity.SysUser;
import com.abc.service.CommonConfigService;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.sql.SQLException;

@PermInfo(value = "数据库操作模块", pval = "a:dbOperator:接口")
@RestController
@RequestMapping("/http/executor")
public class HttpExecutorController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonConfigService commonConfigService;

    @PermInfo("执行HTTP请求")
    @RequiresPermissions("a:http:execute")
    @PostMapping(value = "/execute")
    public Json execute(@RequestBody CommonConfigVo commonConfigVo) throws SQLException {
        String oper = "httpExecute";
        HttpConfig httpConfig = commonConfigVo.getHttpConfig();
        String method = httpConfig.getMethod();
        if (method.equalsIgnoreCase("post")) {
            RequestEntity<String> requestEntity = new RequestEntity<>(httpConfig.getBody(), HttpMethod.resolve(method), URI.create(httpConfig.getUrl()));
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
            return Json.succ(oper, responseEntity);
        }
        return Json.fail(oper, "11111");
    }
}
