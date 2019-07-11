CREATE TABLE `t_variable_config` (
  `name` varchar(50) NOT NULL COMMENT '配置项名称',
  `value` mediumtext DEFAULT NULL COMMENT '配置的值',
  `desc` varchar(128) NOT NULL COMMENT '配置项描述',
  `created` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `updated` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`name`)
) COMMENT='变量配置表,存储一些变量';