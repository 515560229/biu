show tables;

select * from sys_perm;
select * from sys_role;
select * from sys_role_perm;
select * from sys_user;
select * from sys_user_role;
select * from t_variable_config;

SELECT  'name','value','desc',created,updated  FROM sys_user_role LIMIT 0,10;

SELECT COUNT(1) FROM t_variable_config WHERE (`desc` LIKE '%5%');

select * from t_common_config;
