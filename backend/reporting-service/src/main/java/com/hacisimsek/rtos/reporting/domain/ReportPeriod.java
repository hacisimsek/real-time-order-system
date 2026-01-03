package com.hacisimsek.rtos.reporting.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;

public enum ReportPeriod {
    DAILY {
        @Override
        public LocalDate defaultStart(LocalDate endInclusive) {
            return endInclusive.minusDays(6);
        }
    },
    WEEKLY {
        @Override
        public LocalDate defaultStart(LocalDate endInclusive) {
            return endInclusive.minusWeeks(11);
        }
    },
    MONTHLY {
        @Override
        public LocalDate defaultStart(LocalDate endInclusive) {
            return endInclusive.minusMonths(11);
        }
    };

    public abstract LocalDate defaultStart(LocalDate endInclusive);

    @JsonValue
    public String jsonValue() {
        return name();
    }

    @JsonCreator
    public static ReportPeriod fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        return ReportPeriod.valueOf(value.trim().toUpperCase());
    }
}
