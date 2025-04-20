package com.onesteprest.onesteprest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OneStepRest.
 */
@Configuration
@ConfigurationProperties(prefix = "onesteprest")
public class OneStepRestConfig {
    
    private String apiBasePath = "/api";
    private boolean enableGlobalValidation = true;
    private String defaultModelPackage = "com.onesteprest.onesteprest.examples";
    
    public String getApiBasePath() {
        return apiBasePath;
    }
    
    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }
    
    public boolean isEnableGlobalValidation() {
        return enableGlobalValidation;
    }
    
    public void setEnableGlobalValidation(boolean enableGlobalValidation) {
        this.enableGlobalValidation = enableGlobalValidation;
    }
    
    public String getDefaultModelPackage() {
        return defaultModelPackage;
    }
    
    public void setDefaultModelPackage(String defaultModelPackage) {
        this.defaultModelPackage = defaultModelPackage;
    }
}
