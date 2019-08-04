package com.abc.vo;

import com.abc.entity.CommonConfig;
import com.abc.vo.PageVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CommonConfigQueryCondition extends CommonConfig implements PageVo {
    private String key;
    private int current;
    private int size;
    private Boolean onlyCreateByMine;
}
