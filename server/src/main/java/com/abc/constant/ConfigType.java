package com.abc.constant;

public enum ConfigType {
    DB_CONFIG("db"),
    DB_QUERY_CONFIG("dbQuery")
    ;


    private String value;

    private ConfigType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
