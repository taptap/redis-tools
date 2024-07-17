package com.taptap.github.redistools.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author : kl (http://kailing.pub)
 * @since : 2022-03-18 2:29 PM
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.openapi")
public class AliyunOpenApiConfig {

    private Integer connectionTimeoutMillis = 5 * 1000;
    private Integer readTimeoutMillis = 10 * 1000;
    private Integer maxIdleConns = 1000;
    private String accessKeyId;
    private String accessSecret;
    private String regionId = "cn-beijing";
    private String redisEndPoint = "r-kvstore.aliyuncs.com";
    private String redisOpsJobAccountName = "opsjob";
    private String redisOpsJobAccountPassword;
    private boolean redisMetricsEnabled = false;


    @Bean
    public RedisClient redisClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 必填，您的 AccessKey Secret
                .setAccessKeySecret(accessSecret);
        config.setEndpoint(getRedisEndPoint());
        return new RedisClient(config);
    }

    public String getRedisEndPoint() {
        return redisEndPoint;
    }

    public void setRedisEndPoint(String redisEndPoint) {
        this.redisEndPoint = redisEndPoint;
    }

    public Integer getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(Integer connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    public Integer getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(Integer readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public Integer getMaxIdleConns() {
        return maxIdleConns;
    }

    public void setMaxIdleConns(Integer maxIdleConns) {
        this.maxIdleConns = maxIdleConns;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getRedisOpsJobAccountName() {
        return redisOpsJobAccountName;
    }

    public void setRedisOpsJobAccountName(String redisOpsJobAccountName) {
        this.redisOpsJobAccountName = redisOpsJobAccountName;
    }

    public String getRedisOpsJobAccountPassword() {
        return redisOpsJobAccountPassword;
    }

    public void setRedisOpsJobAccountPassword(String redisOpsJobAccountPassword) {
        this.redisOpsJobAccountPassword = redisOpsJobAccountPassword;
    }

    public boolean isRedisMetricsEnabled() {
        return redisMetricsEnabled;
    }

    public void setRedisMetricsEnabled(boolean redisMetricsEnabled) {
        this.redisMetricsEnabled = redisMetricsEnabled;
    }
}
