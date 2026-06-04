package com.dokkcorp.dashboard.config;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExternalCallExecutor {

    private final RetryTemplate retryTemplate;

    public ExternalCallExecutor(
            @Value("${app.http.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.http.retry.backoff-ms:300}") long backoffMs) {
        this.retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        this.retryTemplate.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backoffMs);
        this.retryTemplate.setBackOffPolicy(backOffPolicy);
    }

    public <T> T execute(Supplier<T> supplier) {
        return retryTemplate.execute((RetryCallback<T, RuntimeException>) context -> supplier.get());
    }
}
