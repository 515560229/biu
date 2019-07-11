package com.abc.service.impl;

import com.abc.dao.VariableConfigMapper;
import com.abc.entity.VariableConfig;
import com.abc.service.VariableConfigService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class VariableConfigServiceImpl extends ServiceImpl<VariableConfigMapper, VariableConfig>
        implements VariableConfigService {
}
