package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.util.freemarker.FreemarkerUtils;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
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
    public Json execute(@RequestBody CommonConfigVo commonConfigVo) throws IOException, TemplateException {
        String oper = "httpExecute";
        HttpConfig httpConfig = commonConfigVo.getHttpConfig();

        Map<String, Object> parameterMap = getParameterMap(httpConfig.getParameters());
        //处理header参数
        List<HttpConfig.Header> headers = httpConfig.getHeaders();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (CollectionUtils.isNotEmpty(headers)) {
            for (HttpConfig.Header header : headers) {
                httpHeaders.add(resolve(header.getKey(), parameterMap),
                        resolve(header.getValue(), parameterMap));
            }
        }
        //处理url参数
        String url = resolve(httpConfig.getUrl(), parameterMap);

        HttpMethod httpMethod = HttpMethod.resolve(httpConfig.getMethod());
        RequestEntity<String> requestEntity = null;
        if (httpMethod == HttpMethod.GET) {
            requestEntity = new RequestEntity<>(httpHeaders, httpMethod, URI.create(url));
        } else {
            requestEntity = new RequestEntity<>(resolve(httpConfig.getBody(), parameterMap),
                    httpHeaders,
                    httpMethod, URI.create(url));
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put("request", requestEntity);
        responseObj.put("response", responseEntity);
        return Json.succ(oper, responseObj);
    }

    private Map<String, Object> getParameterMap(List<HttpConfig.Parameter> parameters) throws IOException, TemplateException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (HttpConfig.Parameter parameter : parameters) {
                //参数的值可能是函数
                parameterMap.put(parameter.getName(), FreemarkerUtils.INSTANCE.render(parameter.getDefaultValue(), Collections.emptyMap()));
            }
        }
        return parameterMap;
    }

    private String resolve(String str, Map<String, Object> parameterMap) throws IOException, TemplateException {
        return FreemarkerUtils.INSTANCE.render(str, parameterMap);
    }

}
