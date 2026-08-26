package com.example.fanoutproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fanout")
public class FanoutProperties {

    private String proxyPrefix = "/proxy";
    private int defaultTimeoutMs = 60000;

    public String getProxyPrefix() {
        return proxyPrefix;
    }

    public void setProxyPrefix(String proxyPrefix) {
        this.proxyPrefix = proxyPrefix;
    }

    public int getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(int defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }
}
