DROP TABLE IF EXISTS `t_variable_config`;
CREATE TABLE `t_variable_config`
(
  `name`    varchar(50) NOT NULL COMMENT '配置项名称',
  `value`   mediumtext       DEFAULT NULL COMMENT '配置的值',
  `desc`    varchar(128) COMMENT '配置项描述',
  `created` timestamp   NULL DEFAULT NULL COMMENT '创建时间',
  `updated` timestamp   NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`name`)
) COMMENT ='变量配置表,存储一些变量';
alter table t_variable_config add column `creator`  varchar(50)      DEFAULT NULL COMMENT '创建人';
alter table t_variable_config add column `modifier` varchar(50)      DEFAULT NULL COMMENT '修改人';

DROP TABLE IF EXISTS `t_common_config`;
CREATE TABLE `t_common_config`
(
  `id`       bigint      NOT NULL COMMENT '主键自增长' primary key auto_increment,
  `name`     varchar(128) NOT NULL COMMENT '配置名称',
  `type`     varchar(8)  NOT NULL COMMENT '配置类型',
  `value`    mediumtext       DEFAULT NULL COMMENT '配置的值',
  `desc`     varchar(128) COMMENT '配置项描述',
  `creator`  varchar(50)      DEFAULT NULL COMMENT '创建人',
  `created`  timestamp   NULL DEFAULT NULL COMMENT '创建时间',
  `modifier` varchar(50)      DEFAULT NULL COMMENT '修改人',
  `updated`  timestamp   NULL DEFAULT NULL COMMENT '修改时间'
) COMMENT ='通用配置表';
alter table t_common_config modify column `name` varchar(128) NOT NULL COMMENT '配置名称';

DROP TABLE IF EXISTS `t_kafka_topic`;
CREATE TABLE `t_kafka_topic`
(
  `id`              bigint      NOT NULL COMMENT '主键自增长' primary key auto_increment,
  `name`            varchar(50) NOT NULL COMMENT '主题名称',
  `cluster`         varchar(16) NOT NULL COMMENT '主题所在集群',
  `meta_info`       mediumtext  NOT NULL COMMENT '主题元数据信息',
  `partition_count` int         NOT NULL COMMENT '分区数量',
  `desc`            varchar(128) COMMENT '主题说明',
  `creator`         varchar(50)      DEFAULT NULL COMMENT '创建人',
  `created`         timestamp   NULL DEFAULT NULL COMMENT '创建时间',
  `modifier`        varchar(50)      DEFAULT NULL COMMENT '修改人',
  `updated`         timestamp   NULL DEFAULT NULL COMMENT '修改时间'
) COMMENT ='TOPIC信息表';