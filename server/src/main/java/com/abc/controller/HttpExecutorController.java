package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.util.freemarker.FreemarkerUtils;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
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
import java.nio.charset.Charset;
import java.util.*;

@PermInfo(value = "HTTP模块", pval = "a:http:接口")
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
                if (StringUtils.isNotBlank(header.getKey())) {
                    httpHeaders.add(resolve(header.getKey(), parameterMap),
                            resolve(header.getValue(), parameterMap));
                }
            }
        }
        //处理url参数和urlEncode
        String url = urlEncode(resolve(httpConfig.getUrl(), parameterMap));

        HttpMethod httpMethod = HttpMethod.resolve(httpConfig.getMethod());
        RequestEntity<String> requestEntity = null;
        if (httpMethod == HttpMethod.GET) {
            requestEntity = new RequestEntity<>(httpHeaders, httpMethod, URI.create(url));
        } else {
            if (httpConfig.getBody() != null) {
                requestEntity = new RequestEntity<>(resolve(httpConfig.getBody(), parameterMap),
                        httpHeaders,
                        httpMethod, URI.create(url));
            } else {
                requestEntity = new RequestEntity<>(httpHeaders, httpMethod, URI.create(url));
            }
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

    private String urlEncode(String url) {
        int i = url.indexOf("?");
        if (i < 0) {
            return url;//无参数
        }
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        String paramters = url.substring(i + 1);
        String[] paramValuePairs = paramters.split("&");
        for (String paramValuePair : paramValuePairs) {
            int index2 = paramValuePair.indexOf("=");
            nameValuePairs.add(new BasicNameValuePair(paramValuePair.substring(0, index2), paramValuePair.substring(index2 + 1)));
        }
        String urlParamters = URLEncodedUtils.format(nameValuePairs, Charset.forName("UTF-8"));
        return String.format("%s?%s", url.substring(0, i), urlParamters);
    }
}
