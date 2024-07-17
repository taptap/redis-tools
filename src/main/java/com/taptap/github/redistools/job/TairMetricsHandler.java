package com.taptap.github.redistools.job;

import com.taptap.github.redistools.service.RedisService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * @author kl (http://kailing.pub)
 * @since 2024/1/8
 */
@Component
public class TairMetricsHandler {

    private final RedisService redisService;

    public TairMetricsHandler(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 同步 tair 实例指标，每10分钟执行一次
     */
    @XxlJob(value = "tairMetricsHandler")
    public void handle() {
        redisService.doInitTairMetrics();
    }



}
