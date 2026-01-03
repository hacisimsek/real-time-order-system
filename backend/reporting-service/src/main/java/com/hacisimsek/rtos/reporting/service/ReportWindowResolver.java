package com.hacisimsek.rtos.reporting.service;

import com.hacisimsek.rtos.reporting.domain.ReportPeriod;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReportWindowResolver {

    public ResolvedWindow resolve(ReportPeriod period, LocalDate startDate, LocalDate endDate) {
        ReportPeriod resolvedPeriod = Objects.requireNonNullElse(period, ReportPeriod.DAILY);
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedPeriod.defaultStart(resolvedEnd);

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        return new ResolvedWindow(resolvedPeriod, resolvedStart, resolvedEnd);
    }

    public String cacheKey(ReportPeriod period, LocalDate startDate, LocalDate endDate) {
        ResolvedWindow window = resolve(period, startDate, endDate);
        return cacheKey(window);
    }

    public String cacheKey(ResolvedWindow window) {
        return "%s-%s-%s".formatted(window.period(), window.start(), window.end());
    }

    public record ResolvedWindow(ReportPeriod period, LocalDate start, LocalDate end) {}
}
