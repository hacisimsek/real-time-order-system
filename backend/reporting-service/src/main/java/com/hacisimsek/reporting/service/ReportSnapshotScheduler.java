package com.hacisimsek.reporting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportSnapshotScheduler.class);

    private final ReportSnapshotRefreshService refreshService;
    private final boolean autoRefreshEnabled;

    public ReportSnapshotScheduler(ReportSnapshotRefreshService refreshService,
                                   @Value("${app.reporting.auto-refresh-enabled:true}") boolean autoRefreshEnabled) {
        this.refreshService = refreshService;
        this.autoRefreshEnabled = autoRefreshEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!autoRefreshEnabled) {
            return;
        }
        log.debug("Running startup reporting snapshot refresh");
        refreshService.refresh(null, null);
    }

    @Scheduled(cron = "${app.reporting.refresh-cron:0 0 1 * * *}", zone = "UTC")
    public void refreshNightly() {
        if (!autoRefreshEnabled) {
            return;
        }
        log.debug("Running scheduled reporting snapshot refresh");
        refreshService.refresh(null, null);
    }
}
