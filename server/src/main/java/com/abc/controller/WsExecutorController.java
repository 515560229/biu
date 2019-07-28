package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.util.freemarker.FreemarkerUtils;
import com.abc.util.wsdl.WsdlUtils;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import com.abc.vo.commonconfigvoproperty.WsConfig;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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

@PermInfo(value = "WS模块", pval = "a:ws:接口")
@RestController
@RequestMapping("/ws/executor")
public class WsExecutorController {

    @Autowired
    private RestTemplate restTemplate;

    @PermInfo("执行HTTP请求")
    @RequiresPermissions("a:ws:execute")
    @PostMapping(value = "/execute")
    public Json execute(@RequestBody CommonConfigVo commonConfigVo) throws IOException, TemplateException {
        String oper = "wsExecute";
        WsConfig wsConfig = commonConfigVo.getWsConfig();

        Map<String, Object> parameterMap = getParameterMap(wsConfig.getParameters());
        //处理header参数
        List<WsConfig.Header> headers = wsConfig.getHeaders();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (CollectionUtils.isNotEmpty(headers)) {
            for (WsConfig.Header header : headers) {
                if (StringUtils.isNotBlank(header.getKey())) {
                    httpHeaders.add(resolve(header.getKey(), parameterMap),
                            resolve(header.getValue(), parameterMap));
                }
            }
        }
        //处理url参数
        String url = resolve(wsConfig.getUrl(), parameterMap);

        HttpMethod httpMethod = HttpMethod.POST;
        RequestEntity<String> requestEntity = null;
        requestEntity = new RequestEntity<>(resolve(wsConfig.getBody(), parameterMap),
                httpHeaders,
                httpMethod, URI.create(url));
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put("request", requestEntity);
        responseObj.put("response", responseEntity);
        return Json.succ(oper, responseObj);
    }


    @PermInfo("获取WebService Operation")
    @RequiresPermissions("a:ws:findWsOperations")
    @PostMapping(value = "/findWsOperations")
    public Json findWsOperations(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "findWsOperations";
        WsConfig wsConfig = commonConfigVo.getWsConfig();
        WsdlUtils.SoapPortType soapPortType = null;
        try {
            soapPortType = WsdlUtils.parse(wsConfig.getUrl());
        } catch (Exception ex) {
            // do nothing
        }
        return Json.succ(oper, soapPortType);
    }

    @PermInfo("获取WebService 请求模板")
    @RequiresPermissions("a:ws:getRequestTemplate")
    @PostMapping(value = "/getRequestTemplate")
    public Json getRequestTemplate(@RequestBody CommonConfigVo commonConfigVo) {
        String oper = "getRequestTemplate";
        WsConfig wsConfig = commonConfigVo.getWsConfig();

        WsdlUtils.SoapPortType soapPortType = WsdlUtils.parse(wsConfig.getUrl());
        String soapTemplate = WsdlUtils.createTemplates(wsConfig.getUrl(), soapPortType.getBindingName(), soapPortType.getPortTypeName(), wsConfig.getOperationName());
        return Json.succ(oper, "data", soapTemplate);
    }

    private Map<String, Object> getParameterMap(List<WsConfig.Parameter> parameters) throws IOException, TemplateException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (WsConfig.Parameter parameter : parameters) {
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
