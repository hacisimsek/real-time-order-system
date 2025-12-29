package com.hacisimsek.reporting.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hacisimsek.reporting.cache.ReportCacheNames;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProps.class)
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "app.reporting.cache.provider", havingValue = "caffeine", matchIfMissing = true)
    CacheManager caffeineCacheManager(CacheProps props) {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                ReportCacheNames.REPORT_TOTALS,
                ReportCacheNames.REPORT_TOP_CUSTOMERS);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(props.getMaximumSize())
                .expireAfterWrite(props.getTtl()));
        return manager;
    }

    @Bean
    @ConditionalOnProperty(name = "app.reporting.cache.provider", havingValue = "redis")
    CacheManager redisCacheManager(CacheProps props, RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.hacisimsek.reporting.")
                .allowIfSubType("java.")
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Ensure Redis cache reads return the original DTO types (not LinkedHashMap).
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(props.getTtl())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .initialCacheNames(Set.of(
                        ReportCacheNames.REPORT_TOTALS,
                        ReportCacheNames.REPORT_TOP_CUSTOMERS))
                .build();
    }
}
