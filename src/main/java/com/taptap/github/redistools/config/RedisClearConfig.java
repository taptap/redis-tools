package com.taptap.github.redistools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/11/3
 */
@Configuration
@ConfigurationProperties(prefix = "redis.clear")
public class RedisClearConfig {
    private String sourceAddress = "127.0.0.1";
    private int sourcePort = 6379;
    private String sourcePassword;
    private int sourceDb = 0;
    private int sourceJedisPoolMaxTotal = 200;
    private int sourceJedisPoolMaxIdle = 100;
    private int sourceJedisPoolMinIdle = 100;

    private String bakAddress = "127.0.0.1";
    private String bakPassword;
    private int bakPort = 6379;
    private int bakDb = 0;
    private int bakJedisPoolMaxTotal = 1000;
    private int bakJedisPoolMaxIdle = 300;
    private int bakJedisPoolMinIdle = 200;

    private RunMode mode = RunMode.dry_run;
    private int runThreadNum = 100;
    private int scanBatchSize = 1000;
    private String bakKeyName = "bak";
    private boolean enableBak = false;
    private long bakTtl = TimeUnit.DAYS.toSeconds(7);
    private String scanPattern = "*";
    private long clearMaxIdleTime = 0;
    private long setTtl = 0;

    public RedisClearConfig() {
    }

    public boolean isEnableBak() {
        return enableBak;
    }

    public void setEnableBak(boolean enableBak) {
        this.enableBak = enableBak;
    }

    public int getRunThreadNum() {
        return runThreadNum;
    }

    public void setRunThreadNum(int runThreadNum) {
        this.runThreadNum = runThreadNum;
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

    public String getBakAddress() {
        return bakAddress;
    }

    public void setBakAddress(String bakAddress) {
        this.bakAddress = bakAddress;
    }

    public String getBakPassword() {
        return bakPassword;
    }

    public void setBakPassword(String bakPassword) {
        this.bakPassword = bakPassword;
    }

    public int getBakPort() {
        return bakPort;
    }

    public void setBakPort(int bakPort) {
        this.bakPort = bakPort;
    }

    public int getBakDb() {
        return bakDb;
    }

    public void setBakDb(int bakDb) {
        this.bakDb = bakDb;
    }

    public int getBakJedisPoolMaxTotal() {
        return bakJedisPoolMaxTotal;
    }

    public void setBakJedisPoolMaxTotal(int bakJedisPoolMaxTotal) {
        this.bakJedisPoolMaxTotal = bakJedisPoolMaxTotal;
    }

    public int getBakJedisPoolMaxIdle() {
        return bakJedisPoolMaxIdle;
    }

    public void setBakJedisPoolMaxIdle(int bakJedisPoolMaxIdle) {
        this.bakJedisPoolMaxIdle = bakJedisPoolMaxIdle;
    }

    public int getBakJedisPoolMinIdle() {
        return bakJedisPoolMinIdle;
    }

    public void setBakJedisPoolMinIdle(int bakJedisPoolMinIdle) {
        this.bakJedisPoolMinIdle = bakJedisPoolMinIdle;
    }

    public RunMode getMode() {
        return mode;
    }

    public void setMode(RunMode mode) {
        this.mode = mode;
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

    public long getClearMaxIdleTime() {
        return clearMaxIdleTime;
    }

    public void setClearMaxIdleTime(long clearMaxIdleTime) {
        this.clearMaxIdleTime = clearMaxIdleTime;
    }

    public long getSetTtl() {
        return setTtl;
    }

    public void setSetTtl(long setTtl) {
        this.setTtl = setTtl;
    }

    public String getBakKeyName() {
        return bakKeyName;
    }

    public void setBakKeyName(String bakKeyName) {
        this.bakKeyName = bakKeyName;
    }

    public long getBakTtl() {
        return bakTtl;
    }

    public void setBakTtl(long bakTtl) {
        this.bakTtl = bakTtl;
    }

    public enum RunMode {
        dry_run,
        real_run
    }

}
