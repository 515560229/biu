package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PermInfo(value = "数据库操作模块", pval = "a:dbOperator:接口")
@RestController
@RequestMapping("/http/executor")
public class HttpExecutorController {

    @Autowired
    private RestTemplate restTemplate;

    @PermInfo("执行HTTP请求")
    @RequiresPermissions("a:http:execute")
    @PostMapping(value = "/execute")
    public Json execute(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "httpExecute";
        HttpConfig httpConfig = commonConfigVo.getHttpConfig();

        List<HttpConfig.Header> headers = httpConfig.getHeaders();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (CollectionUtils.isNotEmpty(headers)) {
            for (HttpConfig.Header header : headers) {
                httpHeaders.add(resolve(header.getKey(), httpConfig.getParameters()),
                        resolve(header.getValue(), httpConfig.getParameters()));
            }
        }

        String url = resolve(httpConfig.getUrl(), httpConfig.getParameters());

        HttpMethod httpMethod = HttpMethod.resolve(httpConfig.getMethod());
        RequestEntity<String> requestEntity = null;
        if (httpMethod == HttpMethod.GET) {
            requestEntity = new RequestEntity<>(httpHeaders, httpMethod, URI.create(url));
        } else {
            requestEntity = new RequestEntity<>(resolve(httpConfig.getBody(), httpConfig.getParameters()),
                    httpHeaders,
                    httpMethod, URI.create(url));
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put("request", requestEntity);
        responseObj.put("response", responseEntity);
        return Json.succ(oper, responseObj);
    }

    private String resolve(String str, List<HttpConfig.Parameter> parameters) {
        String result = str;
        for (HttpConfig.Parameter parameter : parameters) {
            result = result.replaceAll("\\$\\{" + parameter.getName() + "?}", parameter.getDefaultValue());
        }
        return result;
    }

}
