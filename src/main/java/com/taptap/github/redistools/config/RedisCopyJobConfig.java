package com.taptap.github.redistools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/10/10
 */
@Configuration
@ConfigurationProperties(prefix = "redis.copy")
public class RedisCopyJobConfig {
    private String sourceAddress = "127.0.0.1:6379";
    private String sourcePassword;
    private String targetAddress = "127.0.0.1:6379";
    private String targetPassword;
    private int targetDb = 0;
    private int targetJedisPoolMaxTotal = 1000;
    private int targetJedisPoolMaxIdle = 300;
    private int targetJedisPoolMinIdle = 200;

    private Boolean sendCommand = false;
    private int sendCommandThreadNum = 10;
    private int sendCommandCount = 1;
    private Boolean filterReadCommand = true;
    private int monitorReTryCount = 1;
    private Boolean enableCompress = false;
    private String compressType = "gzip";
    //gzip 从 1~9 依次压缩级别越来越高，压缩率越来越高，但是压缩时间越来越长
    private int gzipCompressLevel = 5;
    private int gzipCompressBufferSize = 1024;

    private Boolean mgetKeysBatch = false;
    private int mgetQueueStartConsumerThreshold = 10000;
    private int mgetKeysCountBuket1 = 5;
    private int mgetKeysCountBuket2 = 5;
    private int mgetKeysCountBuket3 = 5;
    private int mgetKeysCountBuket4 = 5;
    private int mgetKeysCountBuket5 = 5;
    private int mgetKeysCountBuket6 = 5;
    private int mgetKeysCountBuket7 = 5;
    private int mgetKeysCountBuket8 = 5;
    private int mgetKeysCountBuket9 = 5;
    private int mgetKeysCountBuket10 = 5;

    public RedisCopyJobConfig() {
    }

    public int getGzipCompressLevel() {
        return gzipCompressLevel;
    }

    public void setGzipCompressLevel(int gzipCompressLevel) {
        this.gzipCompressLevel = gzipCompressLevel;
    }

    public int getGzipCompressBufferSize() {
        return gzipCompressBufferSize;
    }

    public void setGzipCompressBufferSize(int gzipCompressBufferSize) {
        this.gzipCompressBufferSize = gzipCompressBufferSize;
    }

    public Boolean getEnableCompress() {
        return enableCompress;
    }

    public void setEnableCompress(Boolean enableCompress) {
        this.enableCompress = enableCompress;
    }

    public String getCompressType() {
        return compressType;
    }

    public void setCompressType(String compressType) {
        this.compressType = compressType;
    }

    public int getMonitorReTryCount() {
        return monitorReTryCount;
    }

    public void setMonitorReTryCount(int monitorReTryCount) {
        this.monitorReTryCount = monitorReTryCount;
    }

    public int getMgetQueueStartConsumerThreshold() {
        return mgetQueueStartConsumerThreshold;
    }

    public void setMgetQueueStartConsumerThreshold(int mgetQueueStartConsumerThreshold) {
        this.mgetQueueStartConsumerThreshold = mgetQueueStartConsumerThreshold;
    }

    public Boolean getMgetKeysBatch() {
        return mgetKeysBatch;
    }

    public void setMgetKeysBatch(Boolean mgetKeysBatch) {
        this.mgetKeysBatch = mgetKeysBatch;
    }

    public int getMgetKeysCountBuket1() {
        return mgetKeysCountBuket1;
    }

    public void setMgetKeysCountBuket1(int mgetKeysCountBuket1) {
        this.mgetKeysCountBuket1 = mgetKeysCountBuket1;
    }

    public int getMgetKeysCountBuket2() {
        return mgetKeysCountBuket2;
    }

    public void setMgetKeysCountBuket2(int mgetKeysCountBuket2) {
        this.mgetKeysCountBuket2 = mgetKeysCountBuket2;
    }

    public int getMgetKeysCountBuket3() {
        return mgetKeysCountBuket3;
    }

    public void setMgetKeysCountBuket3(int mgetKeysCountBuket3) {
        this.mgetKeysCountBuket3 = mgetKeysCountBuket3;
    }

    public int getMgetKeysCountBuket4() {
        return mgetKeysCountBuket4;
    }

    public void setMgetKeysCountBuket4(int mgetKeysCountBuket4) {
        this.mgetKeysCountBuket4 = mgetKeysCountBuket4;
    }

    public int getMgetKeysCountBuket5() {
        return mgetKeysCountBuket5;
    }

    public void setMgetKeysCountBuket5(int mgetKeysCountBuket5) {
        this.mgetKeysCountBuket5 = mgetKeysCountBuket5;
    }

    public int getMgetKeysCountBuket6() {
        return mgetKeysCountBuket6;
    }

    public void setMgetKeysCountBuket6(int mgetKeysCountBuket6) {
        this.mgetKeysCountBuket6 = mgetKeysCountBuket6;
    }

    public int getMgetKeysCountBuket7() {
        return mgetKeysCountBuket7;
    }

    public void setMgetKeysCountBuket7(int mgetKeysCountBuket7) {
        this.mgetKeysCountBuket7 = mgetKeysCountBuket7;
    }

    public int getMgetKeysCountBuket8() {
        return mgetKeysCountBuket8;
    }

    public void setMgetKeysCountBuket8(int mgetKeysCountBuket8) {
        this.mgetKeysCountBuket8 = mgetKeysCountBuket8;
    }

    public int getMgetKeysCountBuket9() {
        return mgetKeysCountBuket9;
    }

    public void setMgetKeysCountBuket9(int mgetKeysCountBuket9) {
        this.mgetKeysCountBuket9 = mgetKeysCountBuket9;
    }

    public int getMgetKeysCountBuket10() {
        return mgetKeysCountBuket10;
    }

    public void setMgetKeysCountBuket10(int mgetKeysCountBuket10) {
        this.mgetKeysCountBuket10 = mgetKeysCountBuket10;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public void setSourcePassword(String sourcePassword) {
        this.sourcePassword = sourcePassword;
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

    public Boolean getSendCommand() {
        return sendCommand;
    }

    public void setSendCommand(Boolean sendCommand) {
        this.sendCommand = sendCommand;
    }

    public Boolean getFilterReadCommand() {
        return filterReadCommand;
    }

    public void setFilterReadCommand(Boolean filterReadCommand) {
        this.filterReadCommand = filterReadCommand;
    }

    public int getSendCommandThreadNum() {
        return sendCommandThreadNum;
    }

    public void setSendCommandThreadNum(int sendCommandThreadNum) {
        this.sendCommandThreadNum = sendCommandThreadNum;
    }

    public int getSendCommandCount() {
        return sendCommandCount;
    }

    public void setSendCommandCount(int sendCommandCount) {
        this.sendCommandCount = sendCommandCount;
    }

    @Override
    public String toString() {
        return "RedisCopyJobConfig{" +
                "sourceAddress='" + sourceAddress + '\'' +
                ", targetAddress='" + targetAddress + '\'' +
                ", targetDb=" + targetDb +
                ", targetJedisPoolMaxTotal=" + targetJedisPoolMaxTotal +
                ", targetJedisPoolMaxIdle=" + targetJedisPoolMaxIdle +
                ", targetJedisPoolMinIdle=" + targetJedisPoolMinIdle +
                ", sendCommand=" + sendCommand +
                ", sendCommandThreadNum=" + sendCommandThreadNum +
                ", sendCommandCount=" + sendCommandCount +
                ", filterReadCommand=" + filterReadCommand +
                ", monitorReTryCount=" + monitorReTryCount +
                ", mgetKeysBatch=" + mgetKeysBatch +
                ", mgetQueueStartConsumerThreshold=" + mgetQueueStartConsumerThreshold +
                ", mgetKeysCountBuket1=" + mgetKeysCountBuket1 +
                ", mgetKeysCountBuket2=" + mgetKeysCountBuket2 +
                ", mgetKeysCountBuket3=" + mgetKeysCountBuket3 +
                ", mgetKeysCountBuket4=" + mgetKeysCountBuket4 +
                ", mgetKeysCountBuket5=" + mgetKeysCountBuket5 +
                ", mgetKeysCountBuket6=" + mgetKeysCountBuket6 +
                ", mgetKeysCountBuket7=" + mgetKeysCountBuket7 +
                ", mgetKeysCountBuket8=" + mgetKeysCountBuket8 +
                ", mgetKeysCountBuket9=" + mgetKeysCountBuket9 +
                ", mgetKeysCountBuket10=" + mgetKeysCountBuket10 +
                '}';
    }
}
