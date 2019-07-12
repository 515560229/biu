package com.abc.service.impl;

import com.abc.dao.CommonConfigMapper;
import com.abc.entity.CommonConfig;
import com.abc.service.CommonConfigService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CommonConfigServiceImpl extends ServiceImpl<CommonConfigMapper, CommonConfig> implements CommonConfigService {
}
