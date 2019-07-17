package com.abc.vo;

import com.abc.entity.CommonConfig;
import com.abc.vo.PageVo;

public class CommonConfigQueryCondition extends CommonConfig implements PageVo {
    private String key;
    private int current;
    private int size;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
