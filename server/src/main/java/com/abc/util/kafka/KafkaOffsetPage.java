package com.abc.util.kafka;

import org.springframework.util.Assert;

public class KafkaOffsetPage {
    private long offsetStart;//可以等,该Offset已有数据
    private long offsetEnd;//不可以等,该offset没有数据, 也就是start和end肯定是不能相等的
    private long pageSize = 10;
    private long pageTotal = -1;

    public static final long[] EMPTY = new long[]{};

    public KafkaOffsetPage(long offsetStart, long offsetEnd) {
        this(offsetStart, offsetEnd, 10);
    }

    public KafkaOffsetPage(long offsetStart, long offsetEnd, long pageSize) {
        this.offsetEnd = offsetEnd;
        this.offsetStart = offsetStart;
        this.pageSize = pageSize;

        if ((offsetEnd - offsetStart) % pageSize == 0) {
            pageTotal = (offsetEnd - offsetStart) / pageSize;
        } else {
            pageTotal = (offsetEnd - offsetStart) / pageSize + 1;
        }
    }

    /**
     * 获取第几页的offset区间, 倒序. 如果该页码不存在, 则返回 EMPTY
     *
     * @param pageIndex
     * @return 左闭右开的区间
     */
    public long[] getOffsetRange(int pageIndex) {
        if (pageTotal == 0) {
            return EMPTY;
        }
        if (pageIndex == pageTotal && pageTotal == 1) {
            return new long[]{offsetStart, offsetEnd};
        }
        if (pageIndex > pageTotal) {
            return EMPTY;
        }
        if (pageIndex == 1) {
            return new long[]{offsetEnd - pageSize, offsetEnd};
        }
        //11
        long left = offsetEnd - pageIndex * pageSize;
        if (left < offsetStart) {
            left = offsetStart;
        }

        return new long[]{left, offsetEnd - (pageIndex - 1) * pageSize};
    }

    public static void main(String[] args) {
        //第1种场景, 仅一页
        KafkaOffsetPage page = new KafkaOffsetPage(0, 5);
        long[] offsetRange = page.getOffsetRange(1);
        System.out.println("only on  one page");
        System.out.println(String.format("%s-%s", offsetRange[0], offsetRange[1]));
        //第2种场景, 多于一页
        System.out.println("over  one page");
        page = new KafkaOffsetPage(0, 11);
        offsetRange = page.getOffsetRange(1);
        System.out.println(String.format("%s-%s", offsetRange[0], offsetRange[1]));
        offsetRange = page.getOffsetRange(2);
        System.out.println(String.format("%s-%s", offsetRange[0], offsetRange[1]));
        //第3种场景  相同，即无数据
        System.out.println("no data.");
        page = new KafkaOffsetPage(5, 5);
        Assert.isTrue(page.getOffsetRange(1) == KafkaOffsetPage.EMPTY);
    }

}
