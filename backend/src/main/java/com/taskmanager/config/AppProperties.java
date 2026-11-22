package com.taskmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String processType = "web";

    /**
     * Deployed app version (shown in X-App-Version, metrics, logs).
     * Bound from {@code APP_VERSION}.
     */
    private String version = "1.0.0";

    /**
     * Probability [0..1] to return HTTP 500 on /api/** (for SLI screencast).
     * Bound from {@code APP_DEMO_FAIL_RATE}.
     */
    private double demoFailRate = 0.0;

    public String getProcessType() {
        return processType;
    }

    public void setProcessType(String processType) {
        this.processType = processType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public double getDemoFailRate() {
        return demoFailRate;
    }

    public void setDemoFailRate(double demoFailRate) {
        this.demoFailRate = demoFailRate;
    }
}
