package com.taptap.github.redistools.job;

import com.taptap.github.redistools.config.RedisClearConfig;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/11/10
 */
@Component
public record RedisClearJobHandler(RedisClearConfig config) {

    private final static Logger logger = LoggerFactory.getLogger(RedisClearJobHandler.class);

    @XxlJob("redisClearJob")
    public void redisClearJob() {
        logger.info("redisClearJob start");
        String startTag = XxlJobHelper.getJobParam();
        if (!"redisClearJob".equals(startTag)) {
            logger.info("redisClearJob end, not enable");
            return;
        }
        String sourceIP = config.getSourceAddress();
        int sourcePort = config.getSourcePort();

        GenericObjectPoolConfig<Jedis> sourceConfig = new GenericObjectPoolConfig<>();
        sourceConfig.setMaxTotal(config.getSourceJedisPoolMaxTotal());
        sourceConfig.setMaxIdle(config.getSourceJedisPoolMaxIdle());
        sourceConfig.setMinIdle(config.getSourceJedisPoolMinIdle());
        sourceConfig.setMaxWait(Duration.ofMillis(-1));


        ExecutorService executorService = new ThreadPoolExecutor(config.getRunThreadNum(), config.getRunThreadNum(), 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        AtomicLong count = new AtomicLong(0);
        try (JedisPool sourceRedisPool = new JedisPool(sourceConfig, sourceIP, sourcePort, 1000 * 60, null, config.getSourcePassword(), config.getSourceDb())) {
            this.jedisPoolWarmUp(sourceRedisPool);
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            //scanParams.match("PLFX-ZZSFP-.*");
            scanParams.match(config.getScanPattern());
            scanParams.count(config.getScanBatchSize());

            Jedis sourceJedis = sourceRedisPool.getResource();
            do {
                ScanResult<String> scanResult = sourceJedis.scan(cursor, scanParams);
                cursor = scanResult.getCursor();
                List<String> keys = scanResult.getResult();
                count.addAndGet(keys.size());
                executorService.execute(new ClearRedis(sourceRedisPool.getResource(), keys, config));
            } while (!"0".equals(cursor));
        } catch (Exception e) {
            logger.error("redisClearJob error", e);
        }
        logger.info("redisClearJob end, scan count:{}", count.get());
    }

    public void jedisPoolWarmUp(JedisPool jedisPool) {
        List<Jedis> jedisList = new ArrayList<>();
        logger.info("jedisPoolWarmUp start");
        for (int i = 0; i < config.getSourceJedisPoolMinIdle(); i++) {
            try {
                Jedis jedis = jedisPool.getResource();
                jedis.ping();
                jedisList.add(jedis);
            } catch (Exception e) {
                logger.error("jedisPoolWarmUp error", e);
            }
        }
        for (Jedis jedis : jedisList) {
            try {
                jedis.close();
            } catch (Exception e) {
                logger.error("jedisPoolWarmUp close", e);
            }
        }
        logger.info("jedisPoolWarmUp end");
    }

    record ClearRedis(Jedis sourceJedis, List<String> keys,
                      RedisClearConfig config) implements Runnable {

        private final static AtomicInteger retry = new AtomicInteger(0);

        @Override
        public void run() {
            try {
                List<String> unlinks = new ArrayList<>();
                for (String key : keys) {
                    try {
                        long idleTime = sourceJedis.objectIdletime(key);
                        if (config.getClearMaxIdleTime() > 0 && idleTime > config.getClearMaxIdleTime()) {
                            if (config.getMode().equals(RedisClearConfig.RunMode.dry_run)) {
                                System.out.println("dryrun_clearKey:" + key + ":" + idleTime + ":" + config.getSourceDb());
                            } else {
                                unlinks.add(key);
                                System.out.println("realrun_clearKey:" + key + ":" + idleTime + ":" + config.getSourceDb());
                            }
                        } else {
                            if (config.getMode().equals(RedisClearConfig.RunMode.dry_run)) {
                                System.out.println("dryrun_ttl:" + key + ":" + idleTime + ":" + config.getSourceDb());
                            } else {
                                System.out.println("realrun_ttl:" + key + ":" + idleTime + ":" + config.getSourceDb());
                                sourceJedis.expire(key, config.getSetTtl());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("clear key error, key:{}", key, e);
                    }
                }
                if (unlinks.size() > 0) {
                    sourceJedis.unlink(unlinks.toArray(new String[0]));
                }
                // bakPipeline.sync();
            } catch (Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignored) {
                }
                if (retry.getAndIncrement() < 3) {
                    this.run();
                } else {
                    logger.error("clear redis error", e);
                }
            } finally {
                sourceJedis.close();
            }

        }
    }
}
