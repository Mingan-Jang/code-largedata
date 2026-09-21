package com.largedata.report.report;

import java.math.BigDecimal;

public record FamilyPartSummary(
        String productGroup,
        String componentCode,
        long recordCount,
        long totalQty,
        BigDecimal totalAmount)
{

}
