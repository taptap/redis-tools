package com.taptap.github.redistools.config;

import com.aliyun.r_kvstore20150101.Client;
import com.aliyun.teaopenapi.models.Config;
/**
 * @author kl (http://kailing.pub)
 * @since 2023/10/16
 */
public class RedisClient extends Client {

    public RedisClient(Config config) throws Exception {
        super(config);
    }
}
