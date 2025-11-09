package com.hacisimsek.reporting.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hacisimsek.reporting.cache.ReportCacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProps.class)
public class CacheConfig {

    @Bean
    CacheManager cacheManager(CacheProps props) {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                ReportCacheNames.REPORT_TOTALS,
                ReportCacheNames.REPORT_TOP_CUSTOMERS);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(props.getMaximumSize())
                .expireAfterWrite(props.getTtl()));
        return manager;
    }
}
