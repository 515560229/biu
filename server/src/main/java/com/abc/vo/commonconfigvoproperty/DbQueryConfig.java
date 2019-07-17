package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

@Data
public class DbQueryConfig {
    private Long id;//dbConfig的id
    private String sqlTemplate;
}
