package com.abc.util;

import com.abc.entity.SysUser;
import org.apache.shiro.SecurityUtils;

public class CurrentUser {

    public static final SysUser get() {
        return (SysUser) SecurityUtils.getSubject().getPrincipal();
    }

    public static final String getUsername() {
        SysUser sysUser = get();
        return sysUser == null ? "noLogin" : sysUser.getUname();
    }

}
