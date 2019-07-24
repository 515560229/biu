package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

import java.util.List;

@Data
public class DbQueryConfig {
    private Long id;//dbConfig的id
    private String sqlTemplate;
    private List<Parameter> parameters;

    @Data
    public static class Parameter {
        private String name;
        private String label;
        private String defaultValue;
    }
}
