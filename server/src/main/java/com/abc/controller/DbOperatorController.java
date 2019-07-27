package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.entity.CommonConfig;
import com.abc.entity.SysUser;
import com.abc.service.CommonConfigService;
import com.abc.util.freemarker.FreemarkerUtils;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.DbQueryConfig;
import com.abc.vo.commonconfigvoproperty.HttpConfig;
import com.baomidou.mybatisplus.plugins.Page;
import com.mysql.jdbc.Driver;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@PermInfo(value = "数据库操作模块", pval = "a:dbOperator:接口")
@RestController
@RequestMapping("/operator/db")
public class DbOperatorController {

    private static final Object MAX_ROWS = 50;
    @Autowired
    private CommonConfigService commonConfigService;

    private Map<String, JdbcTemplate> connectionMap = new ConcurrentHashMap<>();

    private static final int FETCH_MAX_SIZE = 100;

    private JdbcTemplate getJdbcTemplate(SysUser user, CommonConfigVo dbQueryConfig) throws SQLException {
        String dbKey = String.format("%s-%s", user.getUid(), dbQueryConfig.getDbQueryConfig().getId());
        JdbcTemplate jdbcTemplate = connectionMap.get(dbKey);
        if (jdbcTemplate == null) {
            Long dataSourceId = dbQueryConfig.getDbQueryConfig().getId();
            CommonConfig dataSourceConfig = commonConfigService.selectById(dataSourceId);
            jdbcTemplate = new JdbcTemplate(createDataSource(new CommonConfigVo(dataSourceConfig)));
            jdbcTemplate.setFetchSize(FETCH_MAX_SIZE);
            connectionMap.put(dbKey, jdbcTemplate);
        }
        return jdbcTemplate;
    }

    private DataSource createDataSource(CommonConfigVo commonConfigVo) throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setUrl(String.format("jdbc:mysql://%s:%s/%s", commonConfigVo.getDataBaseConfig().getHost(),
                commonConfigVo.getDataBaseConfig().getPort(),
                commonConfigVo.getDataBaseConfig().getDbName()));
        dataSource.setUsername(commonConfigVo.getDataBaseConfig().getUsername());
        dataSource.setPassword(commonConfigVo.getDataBaseConfig().getPassword());
        return dataSource;
    }

    public static void main(String[] args) {
        String sql = "select * from a where a.a = ${a}";
        sql =sql = sql.replaceAll("\\$\\{.*}", "?");
        System.out.println(sql);
    }

    @PermInfo("执行SQL")
    @RequiresPermissions("a:dbOperator:execute")
    @PostMapping(value = "/execute")
    public Json execute(@RequestBody CommonConfigVo commonConfigVo) throws SQLException, IOException, TemplateException {
        String oper = "dbOperateExecute";
        SysUser currentUser = (SysUser) SecurityUtils.getSubject().getPrincipal();

        JdbcTemplate jdbcTemplate = getJdbcTemplate(currentUser, commonConfigVo);

        DbQueryConfig dbQueryConfig = commonConfigVo.getDbQueryConfig();

        List<DbQueryConfig.Parameter> parameters = dbQueryConfig.getParameters();
        Map<String, Object> parameterMap = getParameterMap(parameters);
        // sql = sql.replaceAll("\\$\\{.*?}", "?"); //将${xxx}变为?
        String sql = dbQueryConfig.getSqlTemplate();
        sql = FreemarkerUtils.INSTANCE.render(sql, parameterMap);

        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("executeSQL", sql);
        if (isSelectSQL(sql)) {
            //最多查询100条记录
            List<Map<String, Object>> list = jdbcTemplate.query(getLimitSql(sql), new ColumnMapRowMapper());
            modelMap.put("dataList", list);
            return Json.result("dbOperateExecuteSelect", true, modelMap);
        } else {
            int effectRowCount = jdbcTemplate.update(sql);
            List<Map<String, Object>> list = new ArrayList<>();
            Map<String, Object> obj = new HashMap<>();
            obj.put("result", String.format("受影响的行: %s", effectRowCount));
            list.add(obj);
            modelMap.put("dataList", list);
            return Json.result("dbOperateExecuteUpdate", true, modelMap);
        }
    }

    private Map<String, Object> getParameterMap(List<DbQueryConfig.Parameter> parameters) throws IOException, TemplateException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (DbQueryConfig.Parameter parameter : parameters) {
                //参数的值可能是函数
                parameterMap.put(parameter.getName(), FreemarkerUtils.INSTANCE.render(parameter.getDefaultValue(), Collections.emptyMap()));
            }
        }
        return parameterMap;
    }

    public Object queryOneColumnForSigetonRow(JdbcTemplate jdbcTemplate, String sql, Object[] params, Class cla) {
        Object result = null;
        try {
            if (params == null || params.length > 0) {
                result = jdbcTemplate.queryForObject(sql, params, cla);
            } else {
                result = jdbcTemplate.queryForObject(sql, cla);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public List<Map<String, Object>> queryForMaps(JdbcTemplate jdbcTemplate, String sql, Object[] params) {
        try {
            if (params != null && params.length > 0) {
                return jdbcTemplate.queryForList(sql, params);
            }
            return jdbcTemplate.queryForList(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Page<List<Map<String, Object>>> queryForPage(JdbcTemplate jdbcTemplate, String sql, Object[] params, Page pageParam) {
        int pageSize = pageParam.getSize();
        int pageIndex = pageParam.getCurrent();
        String rowsql = "select count(*) from (" + sql + ") __rowCount__";   //查询总行数sql
        int pages = 0;   //总页数
        int rows = (Integer) queryOneColumnForSigetonRow(jdbcTemplate, rowsql, params, Integer.class);  //查询总行数
        //判断页数,如果是页大小的整数倍就为rows/pageRow如果不是整数倍就为rows/pageRow+1
        if (rows % pageSize == 0) {
            pages = rows / pageSize;
        } else {
            pages = rows / pageSize + 1;
        }
        //查询第page页的数据sql语句
        if (pageIndex <= 1) {
            sql += " limit 0," + pageSize;
        } else {
            sql += " limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
        }
        //查询第page页数据
        List list = queryForMaps(jdbcTemplate, sql, params);

        //返回分页格式数据
        Page<List<Map<String, Object>>> page = new Page<>();
        page.setRecords(list);  //设置显示的当前页数
        page.setTotal(pages);  //设置总页数
        page.setSize(pageSize);   //设置当前页数据
        page.setCurrent(pageIndex);    //设置总记录数
        return page;
    }

    private String getLimitSql(String sql) {
        String trimSQL = sql.trim();
        if (trimSQL.endsWith(";")) {
            return String.format("select * from (%s) __limitTable__ limit %s", trimSQL.substring(0, trimSQL.length() - 1), MAX_ROWS);
        }
        return String.format("select * from (%s) __limitTable__ limit %s", trimSQL, MAX_ROWS);
    }

    private boolean isSelectSQL(String sql) {
        String select = sql.trim().substring(0, 6);
        if (select.equalsIgnoreCase("select")) {
            return true;
        }
        return false;
    }

    private boolean isDMLSQL(String sql) {
        String prefix = sql.trim().substring(0, 6);
        if (prefix.equalsIgnoreCase("update")
                || prefix.equalsIgnoreCase("delete")
                || prefix.equalsIgnoreCase("insert")) {
            return true;
        }
        return false;
    }
}
