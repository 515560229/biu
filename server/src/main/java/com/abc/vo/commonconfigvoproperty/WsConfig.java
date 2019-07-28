package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

import java.util.List;

@Data
public class WsConfig {
    private String url;
    private String operationName;
    private List<Header> headers;
    private String body;
    private List<Parameter> parameters;

    @Data
    public static class Header {
        private String key;
        private String value;
    }

    @Data
    public static class Parameter {
        private String name;
        private String label;
        private String defaultValue;
    }
}
