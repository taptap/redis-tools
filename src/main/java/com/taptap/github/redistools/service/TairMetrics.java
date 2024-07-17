package com.taptap.github.redistools.service;

import com.aliyun.r_kvstore20150101.models.DescribeInstancesOverviewResponseBody.DescribeInstancesOverviewResponseBodyInstances;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kl (http://kailing.pub)
 * @since 2024/1/8
 */
@Component
public class TairMetrics {

    private final MeterRegistry meterRegistry;

    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(TairMetrics.class);
    public final static String METRICS_PREFIX = "aliyuntair.tairpdb.";
    private final static List<String> metricsGroup = List.of("Stats", "Commandstats", "Keyspace", "Rocksdbstats", "Persistence");
    private final static Map<String, Map<String, AtomicDouble>> metricsMap = new ConcurrentHashMap<>();
    private final static List<String> rocksdbMetrics = List.of("num-live-versions", "stalls", "mem-table-flush-pending", "compaction-pending", "writeamp", "readMBps", "writeMBps");
    private final static List<String> skipMetrics = List.of("disk_size_human", "data_used_disk_size_human");

    public TairMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 注册指标, 会注册 {@code metricsGroup} 的指标,已注册的指标不会重复注册
     *
     * @param instances 实例
     */
    public void register(DescribeInstancesOverviewResponseBodyInstances instances, String infoAllStr) {
        String instanceId = instances.getInstanceId();
        String instanceName = instances.getInstanceName();
        final Map<String, Map<String, String>> metricsGroups = parseInfoAllStr(infoAllStr);

        Map<String, AtomicDouble> metrics = metricsMap.get(instanceId);
        if (metrics == null) {
            metrics = new ConcurrentHashMap<>();
            metricsMap.put(instanceId, metrics);
        }
        for (String group : metricsGroup) {
            Map<String, String> metricsMap = metricsGroups.get(group);
            if (metricsMap == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : metricsMap.entrySet()) {
                AtomicDouble metric = metrics.get(entry.getKey());
                if (metric == null) {
                    metric = new AtomicDouble(Double.parseDouble(entry.getValue()));
                    metrics.put(entry.getKey(), metric);
                    this.doRegister(entry.getKey(), instanceId, instanceName, metric, group);
                }
            }
        }
    }

    private void doRegister(String metricKey, String instanceId, String instanceName, AtomicDouble metric, String metricGroup) {

        if ("Commandstats".equals(metricGroup)) {
            //cmdstat_get-rt 结构解析出 get
            String cmdStat = metricKey.split("-")[0];
            String cmdStatPrefix = cmdStat.split("_")[0];
            String command = cmdStat.split("_")[1];
            String baseMetricName = cmdStatPrefix + "_" + metricKey.split("-")[1];
            Gauge.builder(METRICS_PREFIX + baseMetricName, metric, AtomicDouble::get)
                    .tag("instanceId", instanceId)
                    .tag("command", command)
                    .tag("instanceName", instanceName)
                    .description(metricKey)
                    .register(meterRegistry);
        } else if ("Keyspace".equals(metricGroup)) {
            String baseMetricName = "db_" + metricKey.split("-")[1];
            Gauge.builder(METRICS_PREFIX + baseMetricName, metric, AtomicDouble::get)
                    .tag("instanceId", instanceId)
                    .tag("db", metricKey.split("-")[0])
                    .tag("instanceName", instanceName)
                    .description(metricKey)
                    .register(meterRegistry);
        } else if ("Rocksdbstats".equals(metricGroup)) {
            for (String rocksdbMetric : rocksdbMetrics) {
                if (metricKey.startsWith(rocksdbMetric)) {
                    String command = metricKey.replace(rocksdbMetric + "-", "");
                    Gauge.builder(METRICS_PREFIX + rocksdbMetric, metric, AtomicDouble::get)
                            .tag("instanceId", instanceId)
                            .tag("instanceName", instanceName)
                            .tag("command", command)
                            .description(metricKey)
                            .register(meterRegistry);
                    return;
                }
            }

            Gauge.builder(METRICS_PREFIX + metricKey, metric, AtomicDouble::get)
                    .tag("instanceId", instanceId)
                    .tag("instanceName", instanceName)
                    .description(metricKey)
                    .register(meterRegistry);

        } else {
            Gauge.builder(METRICS_PREFIX + metricKey, metric, AtomicDouble::get)
                    .tag("instanceId", instanceId)
                    .tag("instanceName", instanceName)
                    .description(metricKey)
                    .register(meterRegistry);
        }

    }

    public void record(String instanceId, String infoAllStr) {
        final Map<String, Map<String, String>> metricsGroups = parseInfoAllStr(infoAllStr);
        final Map<String, AtomicDouble> metrics = metricsMap.get(instanceId);
        if (metrics == null) {
            return;
        }
        for (String group : metricsGroup) {
            Map<String, String> map = metricsGroups.get(group);
            if (map == null) {
                continue;
            }
            map.forEach((k, v) -> {
                final AtomicDouble metric = metrics.get(k);
                if (metric != null) {
                    metric.set(Double.parseDouble(v));
                }
            });
        }
    }

    public static Map<String, Map<String, String>> parseInfoAllStr(String infoAllStr) {
        Map<String, Map<String, String>> metricsGroup = new HashMap<>();

        String[] lines = infoAllStr.split("\n");
        Map<String, String> metrics = null;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                metrics = new HashMap<>();
                line = line.replace("#", "").trim();
                metricsGroup.put(line, metrics);
                continue;  // 跳过空行和注释行
            }
            String[] parts = line.split(":", 2);
            if (skipMetrics.contains(parts[0])) {
                continue; // 跳过不需要的指标
            }
            if (parts.length == 2) {
                String key = parts[0];
                String value = parts[1];
                String[] values = value.split(",");
                assert metrics != null;
                if (values.length >= 2) {
                    for (String v : values) {
                        String[] kv = v.split("=", 2);
                        if (kv.length == 2) {
                            String v1 = kv[1].replaceAll("[a-zA-Z]", ""); // 去掉单位
                            metrics.put(key + "-" + kv[0], v1);
                        }
                    }
                } else {
                    value = value.replaceAll("[a-zA-Z]", ""); // 去掉单位
                    metrics.put(key, value);
                }

            }
        }

        return metricsGroup;
    }
}
