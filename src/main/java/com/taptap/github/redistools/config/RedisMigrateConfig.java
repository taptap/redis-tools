package com.taptap.github.redistools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/11/3
 */
@Configuration
@ConfigurationProperties(prefix = "redis.migrate")
public class RedisMigrateConfig {
    private String sourceAddress = "127.0.0.1";
    private int sourcePort = 6379;
    private String sourcePassword;
    private int sourceDb = 0;
    private int sourceJedisPoolMaxTotal = 200;
    private int sourceJedisPoolMaxIdle = 100;
    private int sourceJedisPoolMinIdle = 100;


    private String targetAddress = "127.0.0.1";
    private String targetPassword;
    private int targetPort = 6379;
    private int targetDb = 0;
    private int targetJedisPoolMaxTotal = 1000;
    private int targetJedisPoolMaxIdle = 300;
    private int targetJedisPoolMinIdle = 200;

    private boolean enableMigrate = false;
    private boolean enableUnCompressTest = false;
    private boolean enableUnCompress = false;
    private boolean enableTTLHandler = true;
    private int migrateMaxTTLSec = -1;
    private int migrateThreadNum = 10;
    private DataType migrateDataType = DataType.string;
    private int scanBatchSize = 100;
    private String scanPattern = "*";
    private String scanResultKeyFilter = null;
    private int scanMaxKeyNum = 100;

    private CompressType compressType = CompressType.none;

    public boolean isEnableTTLHandler() {
        return enableTTLHandler;
    }

    public void setEnableTTLHandler(boolean enableTTLHandler) {
        this.enableTTLHandler = enableTTLHandler;
    }

    public boolean isEnableUnCompress() {
        return enableUnCompress;
    }

    public void setEnableUnCompress(boolean enableUnCompress) {
        this.enableUnCompress = enableUnCompress;
    }

    public int getScanMaxKeyNum() {
        return scanMaxKeyNum;
    }

    public void setScanMaxKeyNum(int scanMaxKeyNum) {
        this.scanMaxKeyNum = scanMaxKeyNum;
    }

    public DataType getMigrateDataType() {
        return migrateDataType;
    }

    public String getScanResultKeyFilter() {
        return scanResultKeyFilter;
    }

    public void setScanResultKeyFilter(String scanResultKeyFilter) {
        this.scanResultKeyFilter = scanResultKeyFilter;
    }

    public void setMigrateDataType(DataType migrateDataType) {
        this.migrateDataType = migrateDataType;
    }

    public int getMigrateMaxTTLSec() {
        return migrateMaxTTLSec;
    }

    public void setMigrateMaxTTLSec(int migrateMaxTTLSec) {
        this.migrateMaxTTLSec = migrateMaxTTLSec;
    }

    public boolean isEnableMigrate() {
        return enableMigrate;
    }

    public void setEnableMigrate(boolean enableMigrate) {
        this.enableMigrate = enableMigrate;
    }

    public RedisMigrateConfig() {
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public void setSourcePassword(String sourcePassword) {
        this.sourcePassword = sourcePassword;
    }

    public int getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(int sourceDb) {
        this.sourceDb = sourceDb;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getTargetDb() {
        return targetDb;
    }

    public void setTargetDb(int targetDb) {
        this.targetDb = targetDb;
    }

    public int getTargetJedisPoolMaxTotal() {
        return targetJedisPoolMaxTotal;
    }

    public void setTargetJedisPoolMaxTotal(int targetJedisPoolMaxTotal) {
        this.targetJedisPoolMaxTotal = targetJedisPoolMaxTotal;
    }

    public int getTargetJedisPoolMaxIdle() {
        return targetJedisPoolMaxIdle;
    }

    public void setTargetJedisPoolMaxIdle(int targetJedisPoolMaxIdle) {
        this.targetJedisPoolMaxIdle = targetJedisPoolMaxIdle;
    }

    public int getTargetJedisPoolMinIdle() {
        return targetJedisPoolMinIdle;
    }

    public void setTargetJedisPoolMinIdle(int targetJedisPoolMinIdle) {
        this.targetJedisPoolMinIdle = targetJedisPoolMinIdle;
    }

    public CompressType getCompressType() {
        return compressType;
    }

    public void setCompressType(CompressType compressType) {
        this.compressType = compressType;
    }

    public int getMigrateThreadNum() {
        return migrateThreadNum;
    }

    public void setMigrateThreadNum(int migrateThreadNum) {
        this.migrateThreadNum = migrateThreadNum;
    }


    public int getSourceJedisPoolMaxTotal() {
        return sourceJedisPoolMaxTotal;
    }

    public void setSourceJedisPoolMaxTotal(int sourceJedisPoolMaxTotal) {
        this.sourceJedisPoolMaxTotal = sourceJedisPoolMaxTotal;
    }

    public int getSourceJedisPoolMaxIdle() {
        return sourceJedisPoolMaxIdle;
    }

    public void setSourceJedisPoolMaxIdle(int sourceJedisPoolMaxIdle) {
        this.sourceJedisPoolMaxIdle = sourceJedisPoolMaxIdle;
    }

    public int getSourceJedisPoolMinIdle() {
        return sourceJedisPoolMinIdle;
    }

    public void setSourceJedisPoolMinIdle(int sourceJedisPoolMinIdle) {
        this.sourceJedisPoolMinIdle = sourceJedisPoolMinIdle;
    }

    public int getScanBatchSize() {
        return scanBatchSize;
    }

    public void setScanBatchSize(int scanBatchSize) {
        this.scanBatchSize = scanBatchSize;
    }

    public String getScanPattern() {
        return scanPattern;
    }

    public void setScanPattern(String scanPattern) {
        this.scanPattern = scanPattern;
    }

    public boolean isEnableUnCompressTest() {
        return enableUnCompressTest;
    }

    public void setEnableUnCompressTest(boolean enableUnCompressTest) {
        this.enableUnCompressTest = enableUnCompressTest;
    }

    public enum CompressType {
        none,
        gzip,
        snappy,
        zstd
    }

    public enum DataType {
        string,
        hash,
        list,
        set,
        zset
    }
}
