package com.taptap.github.redistools.job;

import com.taptap.github.redistools.config.RedisCopyJobConfig;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.util.ThrowableUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.commands.ProtocolCommand;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/10/7
 */
@Component
public class RedisCopyJobHandler extends RedisCopyJobBase {

    private static final Logger logger = LoggerFactory.getLogger(RedisCopyJobHandler.class);

    private static final ConcurrentLinkedQueue<List<String>> MGET_argsQueue = new ConcurrentLinkedQueue<>();
    private static final AtomicLong MGET_argsQueue_size = new AtomicLong();
    private static Jedis sourceRedis;
    private static final AtomicBoolean start = new AtomicBoolean(false);

    private final RedisCopyJobConfig config;

    public RedisCopyJobHandler(RedisCopyJobConfig config) {
        this.config = config;
    }

    @XxlJob(value = "redisCopy")
    public void start() {
        logger.info("redisCopyJob start");
        String sourceIP = config.getSourceAddress().split(":")[0];
        int sourcePort = Integer.parseInt(config.getSourceAddress().split(":")[1]);

        String targetIP = config.getTargetAddress().split(":")[0];
        int targetPort = Integer.parseInt(config.getTargetAddress().split(":")[1]);
        ExecutorService executorService = Executors.newFixedThreadPool(config.getSendCommandThreadNum());

        GenericObjectPoolConfig<Jedis> jedisPoolConfig = new GenericObjectPoolConfig<>();
        jedisPoolConfig.setMaxTotal(config.getTargetJedisPoolMaxTotal());
        jedisPoolConfig.setMaxIdle(config.getTargetJedisPoolMaxIdle());
        jedisPoolConfig.setMinIdle(config.getTargetJedisPoolMinIdle());
        jedisPoolConfig.setMaxWait(Duration.ofMillis(100));

        try (JedisPool sourceRedisPool = new JedisPool(sourceIP, sourcePort, null, config.getSourcePassword());
             JedisPool targetRedisPool = new JedisPool(jedisPoolConfig, targetIP, targetPort, null, config.getTargetPassword())) {
            start.set(true);
            for (int i = 0; i < config.getMonitorReTryCount() && start.get(); i++) {
                sourceRedis = sourceRedisPool.getResource();
                try {
                    sourceRedis.monitor(new JedisMonitor() {
                        @Override
                        public void onCommand(String command) {
                            if (config.getSendCommand()) {
                                executorService.execute(() -> sendCommand(command, targetRedisPool.getResource()));
                            } else {
                                System.out.println(command);
                            }
                        }
                    });
                } catch (Exception e) {
                    if (!e.getMessage().contains("Socket closed")) {
                        logger.error("redisCopyJob run error", e);
                    }
                } finally {
                    if (sourceRedis != null) {
                        sourceRedis.close();
                    }
                }

            }
        }
        start.set(false);
        executorService.shutdown();
        logger.info("redisCopyJob stop");
    }

    @XxlJob(value = "redisCopyStop")
    public void stop() {
        //必须两次 close 才能终止 monitor，原因暂时不明
        if (sourceRedis != null) {
            sourceRedis.close();
            sourceRedis.close();
            sourceRedis = null;
        }
        start.set(false);
        logger.info("sourceRedis close");
    }

    public void sendCommand(String commandStr, Jedis jedis) {
        String cmd = null;
        List<String> commandAndArgs = null;
        try {
            commandAndArgs = parseCommandAndArgs(commandStr);
            if (commandAndArgs == null) {
                return;
            }
            cmd = commandAndArgs.remove(0);
            if (!redisCommands.contains(cmd.toLowerCase())) {
                logger.warn("redis command not support:{}, commandStr:{}", cmd, commandStr);
                return;
            }
            //mget 命令单独处理，因为集群版本的 redis 实例接收到的 mget 命令参数都是单 key 的
            if ("mget".equalsIgnoreCase(cmd) && config.getMgetKeysBatch()) {
                MGET_argsQueue.add(commandAndArgs);
                if (MGET_argsQueue_size.getAndIncrement() > config.getMgetQueueStartConsumerThreshold()) {
                    commandAndArgs = new ArrayList<>();
                    int mgetBatchSize = this.getRandomBatchSize(config);
                    for (int i = 0; i < mgetBatchSize; i++) {
                        List<String> args = MGET_argsQueue.poll();
                        if (args == null) {
                            continue;
                        }
                        MGET_argsQueue_size.decrementAndGet();
                        commandAndArgs.addAll(args);
                        if (commandAndArgs.size() >= mgetBatchSize) {
                            break;
                        }
                    }
                } else {
                    return;
                }
            }
            //判断是否只发送读命令
            if (config.getFilterReadCommand() && !redisReadCommands.contains(cmd.toLowerCase())) {
                return;
            }

            if (config.getTargetDb() != 0) {
                jedis.select(config.getTargetDb());
            }

            String finalCmd = cmd;
            ProtocolCommand command = () -> finalCmd.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < config.getSendCommandCount(); i++) {
                long startTime = System.nanoTime() / 1000;
                jedis.sendCommand(command, commandAndArgs.toArray(new String[0]));
                System.out.println(cmd + ":" + (System.nanoTime() / 1000 - startTime) + ":" + commandAndArgs);
            }
        } catch (Exception e) {
            logger.error("{}:{}:{}:{}", cmd, commandAndArgs, commandStr, ThrowableUtil.toString(e));
        } finally {
            jedis.close();
        }
    }
}