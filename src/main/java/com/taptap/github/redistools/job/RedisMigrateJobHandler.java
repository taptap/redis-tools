package com.taptap.github.redistools.job;

import com.taptap.github.redistools.config.RedisMigrateConfig;
import com.taptap.github.redistools.utils.Compressions;
import com.taptap.github.redistools.utils.ExceptionUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/11/3
 */
@Component
public record RedisMigrateJobHandler(RedisMigrateConfig config) {

    private static final Logger logger = LoggerFactory.getLogger(RedisMigrateJobHandler.class);

    @XxlJob("redisMigrateJob")
    public void redisMigrateJob() {
        logger.info("redisMigrateJob start");
        String startTag = XxlJobHelper.getJobParam();
        if (!config.isEnableMigrate() || !"redisMigrateJob".equals(startTag)) {
            logger.info("redisMigrateJob end, not enable");
            return;
        }
        String sourceIP = config.getSourceAddress();
        int sourcePort = config.getSourcePort();

        GenericObjectPoolConfig<Jedis> sourceConfig = new GenericObjectPoolConfig<>();
        sourceConfig.setMaxTotal(config.getSourceJedisPoolMaxTotal());
        sourceConfig.setMaxIdle(config.getSourceJedisPoolMaxIdle());
        sourceConfig.setMinIdle(config.getSourceJedisPoolMinIdle());
        sourceConfig.setMaxWait(Duration.ofMillis(-1));

        String targetIP = config.getTargetAddress();
        int targetPort = config().getTargetPort();

        GenericObjectPoolConfig<Jedis> targetConfig = new GenericObjectPoolConfig<>();
        targetConfig.setMaxTotal(config.getTargetJedisPoolMaxTotal());
        targetConfig.setMaxIdle(config.getTargetJedisPoolMaxIdle());
        targetConfig.setMinIdle(config.getTargetJedisPoolMinIdle());
        targetConfig.setMaxWait(Duration.ofMillis(-1));

        ExecutorService executorService = new ThreadPoolExecutor(config.getMigrateThreadNum(), config.getMigrateThreadNum(), 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        AtomicLong count = new AtomicLong(0);
        try (JedisPool sourceRedisPool = new JedisPool(sourceConfig, sourceIP, sourcePort, 1000 * 60, null, config.getSourcePassword(), config.getSourceDb());
             JedisPool targetRedisPool = new JedisPool(targetConfig, targetIP, targetPort, 1000 * 60, null, config.getTargetPassword(), config.getTargetDb())) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams scanParams = new ScanParams();
            //scanParams.match("PLFX-ZZSFP-.*");
            scanParams.match(config.getScanPattern());
            scanParams.count(config.getScanBatchSize());

            Jedis sourceJedis = sourceRedisPool.getResource();
            do {
                if (config.getScanMaxKeyNum() != -1 && count.get() >= config.getScanMaxKeyNum()) {
                    logger.info("scanMaxKeyNum end:{}", config.getScanMaxKeyNum());
                    break;
                }
                ScanResult<String> scanResult = sourceJedis.scan(cursor, scanParams);
                cursor = scanResult.getCursor();
                List<String> keys = scanResult.getResult();
                count.addAndGet(keys.size());
                executorService.execute(new WriteTargetRedis(targetRedisPool.getResource(), keys, sourceRedisPool.getResource(), config));
            } while (!"0".equals(cursor));
        } catch (Exception e) {
            logger.error("redisMigrateJob error:{}", ExceptionUtils.readStackTrace(e));
        }
        logger.info("redisMigrateJob end, scan count:{}", count.get());
    }

    private record WriteTargetRedis(Jedis targetJedis, List<String> keys, Jedis sourceJedis,
                                    RedisMigrateConfig config) implements Runnable {

        @Override
        public void run() {
            try (Pipeline targetPipeline = targetJedis.pipelined()) {

                for (String key : keys) {
                    if (config.getScanResultKeyFilter() != null && key.contains(config.getScanResultKeyFilter())) {
                        System.out.println("key_filter >: " + key);
                        continue;
                    }
                    if (config.getMigrateDataType().equals(RedisMigrateConfig.DataType.string)) {
                        this.writeString(key, targetPipeline, sourceJedis);
                    }
                    if (config.getMigrateDataType().equals(RedisMigrateConfig.DataType.hash)) {
                        this.writeHash(key, targetPipeline, sourceJedis);
                    }
                }

                targetPipeline.sync();

            } catch (Exception e) {
                logger.error("WriteTargetRedis error:{}", ExceptionUtils.readStackTrace(e));
            } finally {
                targetJedis.close();
                sourceJedis.close();
            }

        }

        /**
         * 写入hash类型数据
         *
         * @param key
         * @param targetPipeline
         * @param sourceJedis
         */
        public void writeHash(String key, Pipeline targetPipeline, Jedis sourceJedis) {
            Map<byte[], byte[]> maps;
            try {
                maps = sourceJedis.hgetAll(key.getBytes());
            } catch (Exception e) {
                if (e instanceof JedisDataException) {
                    logger.error("get_key_type_error >: {}", key);
                } else {
                    logger.error("get_key_error >: {}", key, e);
                }
                return;
            }

            if (maps != null && maps.isEmpty()) {
                logger.error("get_key_null >: {}", key);
                return;
            }
            if (config.getCompressType() != RedisMigrateConfig.CompressType.none) {
                if (config.isEnableUnCompress()) {
                    Map<byte[], byte[]> unCompressMaps = new HashMap<>();
                    for (Map.Entry<byte[], byte[]> entry : maps.entrySet()) {
                        if (!isCompressData(entry.getValue())) {
                            System.out.println("hash_key_unCompressed >: " + key + "  , " + "field = " + new String(entry.getKey()));
                            continue;
                        }
                        byte[] value = unCompressData(entry.getValue());
                        unCompressMaps.put(entry.getKey(), value);
                        System.out.println("hash_key_unCompressing >: " + key + "  , " + "field = " + new String(entry.getKey()));
                    }
                    targetPipeline.hmset(key.getBytes(), unCompressMaps);
                } else {
                    Map<byte[], byte[]> compressMaps = new HashMap<>();
                    for (Map.Entry<byte[], byte[]> entry : maps.entrySet()) {
                        if (isCompressData(entry.getValue())) {
                            System.out.println("hash_key_compressed >: " + key + "  , " + "field = " + new String(entry.getKey()));
                            continue;
                        }
                        byte[] value = entry.getValue();
                        value = compressData(value);
                        System.out.println("hash_key_compressing >: " + key + "  , " + "field = " + new String(entry.getKey()));
                        compressMaps.put(entry.getKey(), value);
                    }
                    targetPipeline.hmset(key.getBytes(), compressMaps);
                }

            } else {
                targetPipeline.hmset(key.getBytes(), maps);
                System.out.println("hash_key = " + key);
            }

            // ttl 处理 ，如果 source 和 target 不一致，则需要处理,因为同一个实例的 ttl 不会因为 hmset 导致变化
            if (config.isEnableTTLHandler() && !config.getSourceAddress().equals(config.getTargetAddress())) {
                long ttl = sourceJedis.ttl(key);
                if (ttl > 0) {
                    targetPipeline.expire(key, ttl);
                }
            }

        }

        /**
         * 写入string类型数据
         *
         * @param key
         * @param targetPipeline
         * @param sourceJedis
         */
        public void writeString(String key, Pipeline targetPipeline, Jedis sourceJedis) {
            byte[] value;
            try {
                value = sourceJedis.get(key.getBytes());
            } catch (Exception e) {
                if (e instanceof JedisDataException) {
                    logger.error("get_key_type_error >: {}", key);
                } else {
                    logger.error("get_key_error >: {}", key, e);
                }
                return;
            }
            if (value == null) {
                logger.error("get_key_null >: {}", key);
                return;
            }

            if (config.getCompressType() != RedisMigrateConfig.CompressType.none) {
                if (config.isEnableUnCompress()) {
                    if (isCompressData(value)) {
                        value = unCompressData(value);
                        System.out.println("string_key_unCompressing >: " + key);
                    } else {
                        System.out.println("string_key_unCompressed >: " + key);
                        return;
                    }
                } else {
                    if (isCompressData(value)) {
                        System.out.println("string_key_compressed >: " + key);
                        return;
                    }
                    value = compressData(value);
                    System.out.println("string_key_compressing >: " + key);
                }
            } else {
                System.out.println("string_key >: " + key);
            }
            // ttl 处理，因为 string 的写入始终会影响 ttl ，所以只要开启 ttl 处理，就一定都要处理，无论 source 和 target 是否不一致
            if (config.isEnableTTLHandler()) {
                long ttl = sourceJedis.ttl(key);
                if (ttl <= 0) {
                    targetPipeline.set(key.getBytes(), value);
                } else if (config.getMigrateMaxTTLSec() == -1) {
                    targetPipeline.setex(key.getBytes(), ttl, value);
                } else if (config.getMigrateMaxTTLSec() > 0 && ttl <= config.getMigrateMaxTTLSec()) {
                    targetPipeline.setex(key.getBytes(), ttl, value);
                } else {
                    logger.info("key_ttl_too_long:{},{}", key, ttl);
                }
            } else {
                targetPipeline.set(key.getBytes(), value);
            }

        }

        /**
         * 是否压缩数据
         *
         * @param value
         * @return
         */
        public boolean isCompressData(byte[] value) {
           return Compressions.isZstdData(value) || Compressions.isGzipData(value);
        }

        /**
         * 压缩数据
         *
         * @param value
         * @return
         */
        public byte[] compressData(byte[] value) {
            if (config.getCompressType() == RedisMigrateConfig.CompressType.zstd) {
                long start = System.nanoTime() / 1000;
                long oldLength = value.length;
                value = Compressions.zstdCompress(value);
                System.out.println("zstd:" + (System.nanoTime() / 1000 - start) + ":" + oldLength + ":" + value.length);
                if (config.isEnableUnCompressTest()) {
                    start = System.nanoTime() / 1000;
                    Compressions.zstdUnCompress(value);
                    System.out.println("unCompress:" + (System.nanoTime() / 1000 - start));
                }
            }

            if (config.getCompressType() == RedisMigrateConfig.CompressType.gzip) {
                long start = System.nanoTime() / 1000;
                long oldLength = value.length;
                value = Compressions.gzipCompress(value);

                System.out.println("gzip:" + (System.nanoTime() / 1000 - start) + ":" + oldLength + ":" + value.length);
                if (config.isEnableUnCompressTest()) {
                    start = System.nanoTime() / 1000;
                    Compressions.gzipUnCompress(value);
                    System.out.println("unCompress:" + (System.nanoTime() / 1000 - start));
                }
            }
            return value;
        }

        /**
         * 解压缩数据
         *
         * @param value
         * @return
         */
        public byte[] unCompressData(byte[] value) {
            if (config.getCompressType() == RedisMigrateConfig.CompressType.zstd) {
                long start = System.nanoTime() / 1000;
                value = Compressions.zstdUnCompress(value);
                System.out.println("unCompress:" + (System.nanoTime() / 1000 - start));
            }

            if (config.getCompressType() == RedisMigrateConfig.CompressType.gzip) {
                long start = System.nanoTime() / 1000;
                value = Compressions.gzipUnCompress(value);
                System.out.println("unCompress:" + (System.nanoTime() / 1000 - start));
            }
            return value;
        }
    }
}
