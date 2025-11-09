package com.hacisimsek.reporting.service;

import com.hacisimsek.reporting.dto.ReportSummaryResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportCsvExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    public byte[] toCsv(List<ReportSummaryResponse> summaries) {
        StringBuilder builder = new StringBuilder();
        builder.append("period,snapshotDate,totalOrders,totalRevenueCents,totalRevenue").append('\n');
        for (ReportSummaryResponse summary : summaries) {
            builder.append(summary.period()).append(',')
                    .append(DATE_FORMATTER.format(summary.snapshotDate())).append(',')
                    .append(summary.totalOrders()).append(',')
                    .append(summary.totalRevenueCents()).append(',')
                    .append(summary.totalRevenue().toPlainString())
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
