package com.taskmanager.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AppMetrics {

    private final Counter authLogins;
    private final Counter authRegistrations;
    private final Counter authFailedLogins;
    private final Counter tasksCreated;
    private final Counter tasksCompleted;

    public AppMetrics(MeterRegistry registry) {
        authLogins = Counter.builder("taskmanager.auth.logins")
                .description("Successful user logins")
                .register(registry);
        authRegistrations = Counter.builder("taskmanager.auth.registrations")
                .description("Successful user registrations")
                .register(registry);
        authFailedLogins = Counter.builder("taskmanager.auth.failed_logins")
                .description("Failed login attempts")
                .register(registry);
        tasksCreated = Counter.builder("taskmanager.tasks.created")
                .description("Tasks created")
                .register(registry);
        tasksCompleted = Counter.builder("taskmanager.tasks.completed")
                .description("Tasks moved to DONE")
                .register(registry);
    }

    public void incAuthLogins() {
        authLogins.increment();
    }

    public void incAuthRegistrations() {
        authRegistrations.increment();
    }

    public void incAuthFailedLogins() {
        authFailedLogins.increment();
    }

    public void incTasksCreated() {
        tasksCreated.increment();
    }

    public void incTasksCompleted() {
        tasksCompleted.increment();
    }
}
