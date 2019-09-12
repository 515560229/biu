package com.abc.util;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Date;

public class DateTimeUtils {
    private DateTimeUtils() {
        //do nothing
    }

    public static Date parse(String dateStr, String pattern) throws ParseException {
        if (pattern.equals("timestamp")) {
            return new Date(Long.valueOf(dateStr));
        }
        return DateUtils.parseDate(dateStr, pattern);
    }

    public static String formatToString(Date date, String pattern) {
        if (pattern.equals("timestamp")) {
            return String.valueOf(date.getTime());
        }
        return DateFormatUtils.format(date, pattern);
    }

    public static String add(String pattern, Date date, int amount, String unit) {
        if (unit.equalsIgnoreCase("year") || unit.equalsIgnoreCase("年")) {
            return formatToString(DateUtils.addYears(date, amount), pattern);
        }
        if (unit.equalsIgnoreCase("month") || unit.equalsIgnoreCase("月")) {
            return formatToString(DateUtils.addMonths(date, amount), pattern);
        }
        if (unit.equalsIgnoreCase("day") || unit.equalsIgnoreCase("日")) {
            return formatToString(DateUtils.addDays(date, amount), pattern);
        }
        if (unit.equalsIgnoreCase("hour") || unit.equalsIgnoreCase("时")) {
            return formatToString(DateUtils.addHours(date, amount), pattern);
        }
        if (unit.equalsIgnoreCase("minute") || unit.equalsIgnoreCase("分")) {
            return formatToString(DateUtils.addMinutes(date, amount), pattern);
        }
        if (unit.equalsIgnoreCase("second") || unit.equalsIgnoreCase("秒")) {
            return formatToString(DateUtils.addSeconds(date, amount), pattern);
        }
        return formatToString(DateUtils.addMilliseconds(date, amount), pattern);
    }
}
