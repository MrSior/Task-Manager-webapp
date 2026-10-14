package com.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagerApplication {

    public static void main(String[] args) {
        String processType = System.getenv().getOrDefault("APP_PROCESS_TYPE", "web").trim().toLowerCase();
        SpringApplication app = new SpringApplication(TaskManagerApplication.class);
        if (!"web".equals(processType)) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        if ("worker".equals(processType) || "cron".equals(processType)) {
            // Keep non-web processes alive until SIGTERM (12-factor process model).
            System.setProperty("spring.main.keep-alive", "true");
        }
        app.run(args);
    }
}
