package com.taptap.github.redistools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/20
 */
@Configuration
@ConfigurationProperties(XxlJobProperties.PREFIX)
public class XxlJobProperties {

  public static final String PREFIX = "xxl.job";

  private String adminAddresses;
  private String appName;
  private String address;
  private String appTitle;
  private Boolean enabled;
  private String ip;
  private int port;
  private String logPath;
  private int logRetentionDays;


  public String getAdminAddresses() {
    return adminAddresses;
  }

  public void setAdminAddresses(String adminAddresses) {
    this.adminAddresses = adminAddresses;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getLogPath() {
    return logPath;
  }

  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }

  public int getLogRetentionDays() {
    return logRetentionDays;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppTitle() {
    return appTitle;
  }

  public void setAppTitle(String appTitle) {
    this.appTitle = appTitle;
  }

  public void setLogRetentionDays(int logRetentionDays) {
    this.logRetentionDays = logRetentionDays;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
