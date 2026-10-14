package com.taskmanager.process;

import com.taskmanager.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ProcessBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessBootstrap.class);

    private final AppProperties appProperties;
    private final ConfigurableApplicationContext context;

    public ProcessBootstrap(AppProperties appProperties, ConfigurableApplicationContext context) {
        this.appProperties = appProperties;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        String processType = normalize(appProperties.getProcessType());
        log.info("Starting process type={}", processType);

        switch (processType) {
            case "migrate" -> {
                log.info("Database migrations completed; migrate process exiting");
                int code = SpringApplication.exit(context, () -> 0);
                System.exit(code);
            }
            case "worker" -> log.info("Worker process is running and will shut down gracefully on SIGTERM");
            case "cron" -> log.info("Cron process is running and will shut down gracefully on SIGTERM");
            case "web" -> log.info("Web process is ready to accept HTTP traffic");
            default -> throw new IllegalStateException("Unknown APP_PROCESS_TYPE: " + processType);
        }
    }

    private String normalize(String value) {
        return value == null ? "web" : value.trim().toLowerCase();
    }
}
