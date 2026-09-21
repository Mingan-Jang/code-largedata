package com.largedata.report.report;

import java.time.YearMonth;

public record AvailableReportMonth(YearMonth month, int productGroupCount, long recordCount) {

    public String value() {
        return month.toString();
    }
}
