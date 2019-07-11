package com.abc.vo;

import com.abc.entity.VariableConfig;

public class VariableQueryConditionVo extends VariableConfig implements PageVo {
    private int current;
    private int size;

    public void setCurrent(int current) {
        this.current = current;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int getCurrent() {
        return this.current;
    }

    @Override
    public int getSize() {
        return this.size;
    }
}
