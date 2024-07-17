package com.taptap.github.redistools.service;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.r_kvstore20150101.models.CreateAccountRequest;
import com.aliyun.r_kvstore20150101.models.CreateAccountResponse;
import com.aliyun.r_kvstore20150101.models.DescribeAccountsRequest;
import com.aliyun.r_kvstore20150101.models.DescribeAccountsResponseBody.DescribeAccountsResponseBodyAccountsAccount;
import com.aliyun.r_kvstore20150101.models.DescribeInstancesOverviewRequest;
import com.aliyun.r_kvstore20150101.models.DescribeInstancesOverviewResponseBody.DescribeInstancesOverviewResponseBodyInstances;
import com.aliyun.r_kvstore20150101.models.DescribeParametersRequest;
import com.aliyun.r_kvstore20150101.models.DescribeParametersResponseBody;
import com.aliyun.r_kvstore20150101.models.ModifyInstanceConfigRequest;
import com.aliyun.r_kvstore20150101.models.ModifyInstanceConfigResponse;
import com.taptap.github.redistools.config.AliyunOpenApiConfig;
import com.taptap.github.redistools.config.RedisClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author kl (http://kailing.pub)
 * @since 2024/1/9
 */
@Component
public class RedisService {

    private final RedisClient redisClient;
    private final AliyunOpenApiConfig config;
    private final TairMetrics tairMetrics;
    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(RedisService.class);
    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private final static Map<String, JedisPool> jedisMap = new ConcurrentHashMap<>();
    private final Map<String, DescribeInstancesOverviewResponseBodyInstances> instancesMap = new ConcurrentHashMap<>();

    public RedisService(RedisClient redisClient, AliyunOpenApiConfig config, TairMetrics tairMetrics) {
        this.redisClient = redisClient;
        this.config = config;
        this.tairMetrics = tairMetrics;
        new Thread(this::init).start();
    }

    /**
     * 初始化：
     * 1、创建 ops-job 监控账号
     * 2、创建 Redis 链接 map
     * 3、定时任务记录指标
     */
    private void init() {
        if (config.isRedisMetricsEnabled()) {
            this.doInitTairMetrics();
            scheduledExecutorService.scheduleWithFixedDelay(
                    new RecordMetricsTask(jedisMap, config, instancesMap, tairMetrics),
                    1,
                    10,
                    TimeUnit.SECONDS);
        }
    }

    public void doInitTairMetrics() {
        List<DescribeInstancesOverviewResponseBodyInstances> instances = this.getInstancesByTair();
        if (instances == null || instances.size() == 0) {
            logger.warn("未查询到redis实例");
            return;
        }
        if (instancesMap.size() == instances.size()) {
            logger.info("tair实例数量未发生变化: {}", instances.size());
            // 实例数量未发生变化, 不需要重新初始化, 只需要更新指标 map
            for (Map.Entry<String, JedisPool> entry : jedisMap.entrySet()) {
                final DescribeInstancesOverviewResponseBodyInstances instance = instancesMap.get(entry.getKey());
                try (final Jedis jedis = entry.getValue().getResource()) {
                    tairMetrics.register(instance, jedis.info("all"));
                } catch (Exception e) {
                    logger.warn("tair实例指标注册失败, instanceName: {}", instance.getInstanceName(), e);
                }
            }
            return;
        }
        logger.info("tair实例数量: {}", instances.size());
        // 实例数量发生变化, 注册新实例
        for (DescribeInstancesOverviewResponseBodyInstances instance : instances) {
            String instanceId = instance.getInstanceId();
            if (instancesMap.putIfAbsent(instanceId, instance) == null) {
                if (!this.opsJobAccountIsExist(instance.getInstanceId())) {
                    boolean crateResult = this.createOpsJobAccount(instance.getInstanceId());
                    if (!crateResult) {
                        logger.error("创建ops-job监控账号失败, instanceId: {}", instance.getInstanceId());
                    }
                }
                logger.info("tair实例指标注册进行中: {}", instanceId);
                JedisPool pool = getJedis(instance, config);
                try (Jedis jedis = pool.getResource()) {
                    tairMetrics.register(instance, jedis.info("all"));
                    jedisMap.put(instanceId, pool);
                    logger.info("tair实例指标注册完成: {}", instanceId);
                } catch (Exception e) {
                    instancesMap.remove(instanceId);
                    logger.error("tair实例指标注册失败,instanceId:{}, url: {}", instanceId, instance.getConnectionDomain(), e);
                }
            }
        }
        logger.info("tair实例指标初始化完成, 实例数量: {}", jedisMap.size());
    }


    record RecordMetricsTask(Map<String, JedisPool> jedisMap,
                             AliyunOpenApiConfig config,
                             Map<String, DescribeInstancesOverviewResponseBodyInstances> instancesMap,
                             TairMetrics tairMetrics) implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            for (Map.Entry<String, JedisPool> entry : jedisMap.entrySet()) {
                final String instanceId = entry.getKey();
                try (final Jedis jedis = entry.getValue().getResource()) {
                    final String infoAllStr = jedis.info("all");
                    if (infoAllStr == null) {
                        logger.warn("获取redis指标失败, instanceId: {}", instanceId);
                        continue;
                    }
                    tairMetrics.record(instanceId, infoAllStr);
                } catch (Exception e) {
                    logger.error("redis指标失败,instanceId: {}", instanceId, e);
                }
            }
            logger.info("redis指标记录耗时: {}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 获取 Jedis 实例
     *
     * @return 实例
     */
    private static JedisPool getJedis(DescribeInstancesOverviewResponseBodyInstances instances, AliyunOpenApiConfig config) {
        GenericObjectPoolConfig<Jedis> sourceConfig = new GenericObjectPoolConfig<>();
        sourceConfig.setMaxTotal(3);
        sourceConfig.setMaxIdle(2);
        sourceConfig.setMinIdle(2);
        sourceConfig.setMaxWait(Duration.ofSeconds(2));
        return new JedisPool(sourceConfig, instances.getConnectionDomain(), 6379, 2*1000, config.getRedisOpsJobAccountName(), config.getRedisOpsJobAccountPassword());
    }

    /**
     * 创建 ops-job 监控账号
     *
     * @param instanceId 实例ID
     * @return 是否创建成功
     */
    public boolean createOpsJobAccount(String instanceId) {
        try {
            CreateAccountResponse response = redisClient.createAccount(new CreateAccountRequest()
                    .setAccountName(config.getRedisOpsJobAccountName())
                    .setAccountPrivilege("RoleReadOnly")
                    .setAccountType("Normal")
                    .setAccountDescription("ops-job 监控账号, https://xindong.atlassian.net/browse/TAP-31805")
                    .setAccountPassword(config.getRedisOpsJobAccountPassword())
                    .setInstanceId(instanceId));
            return response.getBody() != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询 ops-job 监控账号是否存在
     *
     * @param instanceId 实例ID
     * @return 是否存在
     */
    public boolean opsJobAccountIsExist(String instanceId) {
        List<DescribeAccountsResponseBodyAccountsAccount> accounts = this.queryAccount(config.getRedisOpsJobAccountName(), instanceId);
        return accounts != null && accounts.size() > 0;
    }

    /**
     * 查询账号
     *
     * @param accountName 账号名
     * @param instanceId  实例ID
     * @return 账号信息
     */
    public List<DescribeAccountsResponseBodyAccountsAccount> queryAccount(String accountName, String instanceId) {
        DescribeAccountsRequest request = new DescribeAccountsRequest()
                .setAccountName(accountName)
                .setInstanceId(instanceId);
        try {
            return redisClient.describeAccounts(request)
                    .getBody()
                    .getAccounts()
                    .getAccount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取 Redis 实例列表
     *
     * @return 实例列表
     */
    public List<DescribeInstancesOverviewResponseBodyInstances> getInstancesByTair() {
        DescribeInstancesOverviewRequest describeInstancesOverviewRequest = new DescribeInstancesOverviewRequest()
                .setInstanceType("Tair")
                .setInstanceStatus("Normal")
                .setArchitectureType("standard")
                .setRegionId("cn-beijing");
        try {
            List<DescribeInstancesOverviewResponseBodyInstances> instances = redisClient.describeInstancesOverview(describeInstancesOverviewRequest)
                    .getBody()
                    .getInstances();
            describeInstancesOverviewRequest.setRegionId("cn-hongkong");
            instances.addAll(redisClient.describeInstancesOverview(describeInstancesOverviewRequest)
                    .getBody()
                    .getInstances());

            return instances
                    .stream()
                    .filter(instance -> instance.getInstanceStatus().equals("Normal")
                            || instance.getInstanceStatus().equals("MinorVersionUpgrading"))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置实例参数
     *
     * @param instanceId     实例ID
     * @param parameterName  参数名
     * @param parameterValue 参数值
     */
    public ModifyInstanceConfigResponse setParameter(String instanceId, String parameterName, Object parameterValue) throws Exception {
        JSONObject config = new JSONObject();
        config.put(parameterName, parameterValue);
        ModifyInstanceConfigRequest modifyInstanceConfigRequest = new ModifyInstanceConfigRequest()
                .setConfig(config.toJSONString())
                .setInstanceId(instanceId);
        return redisClient.modifyInstanceConfig(modifyInstanceConfigRequest);
    }

    /**
     * 获取实例参数
     *
     * @param instanceId    实例ID
     * @param parameterName 参数名
     * @return 参数值
     */
    public DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter getParameterValue(String instanceId, String parameterName) {
        List<DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter> parameters = this.getParameters(instanceId);
        for (DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter parameter : parameters) {
            if (parameter.getParameterName().equals(parameterName)) {
                return parameter;
            }
        }
        return null;
    }

    /**
     * 获取实例参数列表
     *
     * @param instanceId 实例ID
     * @return 参数列表
     */
    private List<DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter> getParameters(String instanceId) {
        DescribeParametersRequest describeParametersRequest = new DescribeParametersRequest()
                .setDBInstanceId(instanceId);
        List<DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter> parameters = null;
        try {
            parameters = redisClient.describeParameters(describeParametersRequest)
                    .getBody()
                    .getRunningParameters()
                    .getParameter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parameters;
    }
}
