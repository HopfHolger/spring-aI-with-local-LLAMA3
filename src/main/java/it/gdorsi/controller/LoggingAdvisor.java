package it.gdorsi.controller;

import org.springframework.ai.chat.client.advisor.api.Advisor;

public class LoggingAdvisor implements Advisor {
    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
