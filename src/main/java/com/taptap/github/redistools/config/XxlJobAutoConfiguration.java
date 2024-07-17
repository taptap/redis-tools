package com.taptap.github.redistools.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.yaml.snakeyaml.nodes.Tag.PREFIX;


/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/20
 */
@Configuration
@ConditionalOnProperty(prefix = PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
@Import(com.taptap.github.redistools.config.XxlJobProperties.class)
public class XxlJobAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(XxlJobAutoConfiguration.class);

  @Bean
  public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties props) {
    logger.info(">>>>>>>>>>> xxl-job config init.");
    XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
    xxlJobSpringExecutor.setAdminAddresses(props.getAdminAddresses());
    xxlJobSpringExecutor.setAppname(props.getAppName());
    xxlJobSpringExecutor.setAddress(props.getAddress());
    xxlJobSpringExecutor.setIp(props.getIp());
    xxlJobSpringExecutor.setPort(props.getPort());
    xxlJobSpringExecutor.setLogPath(props.getLogPath());
    return xxlJobSpringExecutor;
  }
}
