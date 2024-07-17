package com.taptap.github.redistools.job;

import com.alibaba.fastjson.JSON;
import com.aliyun.r_kvstore20150101.models.DescribeParametersResponseBody;
import com.aliyun.r_kvstore20150101.models.ModifyInstanceConfigResponse;
import com.taptap.github.redistools.service.RedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/10/16
 */
@Component
public class RedisConfigJobHandler {

    private final RedisService redisService;

    public RedisConfigJobHandler(RedisService redisService) {
        this.redisService = redisService;
    }

    @XxlJob(value = "redisConfigJobHandler")
    public void redisConfigJobHandler() {
        String param = XxlJobHelper.getJobParam();
        RedisConfig redisConfig = JSON.parseObject(param, RedisConfig.class);
        DescribeParametersResponseBody.DescribeParametersResponseBodyRunningParametersParameter runtimeParameter = redisService.getParameterValue(redisConfig.getInstanceId(), redisConfig.getParameterName());

        /*
          job 校验
          1. 参数是否存在
          2. 参数值是否一致
          3. 是否需要重启实例
          4. 是否可修改
         */
        Assert.notNull(runtimeParameter, redisConfig.getParameterName() + ":参数不存在");
        Assert.isTrue(!redisConfig.getParameterValue().equals(runtimeParameter.getParameterValue()),redisConfig.getParameterName() + ":参数值一致,无需修改");
        Assert.isTrue("false".equalsIgnoreCase(runtimeParameter.getForceRestart()),redisConfig.getParameterName() + ":参数需要重启实例才能生效,请确认是否需要修改");
        Assert.isTrue("true".equalsIgnoreCase(runtimeParameter.getModifiableStatus()),redisConfig.getParameterName() + ":参数不可修改");

        try {
            ModifyInstanceConfigResponse response = redisService.setParameter(redisConfig.getInstanceId(), redisConfig.getParameterName(), redisConfig.getParameterValue());
            Assert.notNull(response.getBody(), redisConfig.getParameterName() + ":参数修改失败");
            XxlJobHelper.log(redisConfig.getParameterName() + " :参数修改成功, requestId: "+response.getBody().getRequestId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * redis配置: {"instanceId":"r-2zejgsekf4ux3xxxx","parameterName":"maxmemory","parameterValue":"100000000"}
     */
    static class RedisConfig {
        private String instanceId;
        private String parameterName;
        private Object parameterValue;

        public RedisConfig() {
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getParameterName() {
            return parameterName;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public Object getParameterValue() {
            return parameterValue;
        }

        public void setParameterValue(Object parameterValue) {
            this.parameterValue = parameterValue;
        }
    }
}
