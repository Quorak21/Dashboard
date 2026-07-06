package com.dokkcorp.dashboard.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a singleton {@link Clock} bean for the application.
 *
 * <p>Using UTC eliminates timezone-sensitive boundary flips (e.g., assets near the 90-day
 * staleness threshold behaving differently across dev / CI / prod environments).
 *
 * <p>Inject {@code Clock} in services and use {@code LocalDate.now(clock)} rather than
 * {@code LocalDate.now()} to guarantee reproducible, mockable time in tests.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
