package com.ibm.websphere.samples.daytrader.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daytrader")
public class ApplicationProps {
    private int maxQuotes = 1000;
    private int maxUsers = 500;

    public int getMaxQuotes() {
        return maxQuotes;
    }

    public void setMaxQuotes(int maxQuotes) {
        this.maxQuotes = maxQuotes;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }
}