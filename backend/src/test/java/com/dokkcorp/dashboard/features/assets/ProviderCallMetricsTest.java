package com.dokkcorp.dashboard.features.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

class ProviderCallMetricsTest {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger metricsLogger;

    @BeforeEach
    void setUp() {
        metricsLogger = (Logger) LoggerFactory.getLogger(ProviderCallMetrics.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        metricsLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        metricsLogger.detachAppender(logAppender);
    }

    @Test
    void incrementFmpAndScrape_updateCounters() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, Clock.systemUTC());

        metrics.incrementFmp();
        metrics.incrementFmp();
        metrics.incrementScrape();
        metrics.recordScrapeFailure();

        assertEquals(2, metrics.fmpCalls());
        assertEquals(1, metrics.scrapeCalls());
        assertEquals(1, metrics.scrapeFailures());
    }

    @Test
    void logMetrics_emitsInfoLinesWithCounts() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, Clock.systemUTC());
        metrics.incrementFmp();
        metrics.incrementScrape();

        metrics.logMetrics();

        List<String> messages = logMessages();
        assertTrue(messages.stream().anyMatch(m -> m.contains("FMP calls today: 1")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Scrape calls today: 1")));
    }

    @Test
    void logMetrics_warnsWhenFmpExceedsThreshold() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(5, Clock.systemUTC());
        for (int i = 0; i < 6; i++) {
            metrics.incrementFmp();
        }

        metrics.logMetrics();

        assertTrue(warnMessages().stream()
                .anyMatch(m -> m.contains("FMP calls today: 6") && m.contains("approaching daily limit")));
    }

    @Test
    void logMetrics_warnsWhenScrapeFailureRateExceedsTwentyPercent() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, Clock.systemUTC());
        for (int i = 0; i < 5; i++) {
            metrics.incrementScrape();
        }
        for (int i = 0; i < 2; i++) {
            metrics.incrementScrape();
            metrics.recordScrapeFailure();
        }

        metrics.logMetrics();

        assertTrue(warnMessages().stream()
                .anyMatch(m -> m.contains("Scrape failure rate") && m.contains("above 20% threshold")));
    }

    @Test
    void logMetrics_warnsWhenAllScrapeAttemptsFail() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, Clock.systemUTC());
        for (int i = 0; i < 3; i++) {
            metrics.incrementScrape();
            metrics.recordScrapeFailure();
        }

        metrics.logMetrics();

        assertTrue(warnMessages().stream()
                .anyMatch(m -> m.contains("Scrape failure rate") && m.contains("above 20% threshold")));
    }

    @Test
    void logMetrics_noWarnBelowThresholds() {
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, Clock.systemUTC());
        for (int i = 0; i < 100; i++) {
            metrics.incrementFmp();
        }

        metrics.logMetrics();

        assertTrue(warnMessages().isEmpty());
    }

    @Test
    void countersResetOnNewUtcDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-23T12:00:00Z"), UTC);
        ProviderCallMetrics metrics = ProviderCallMetrics.createForTest(200, clock);
        metrics.incrementFmp();
        metrics.incrementScrape();
        metrics.recordScrapeFailure();
        assertEquals(1, metrics.fmpCalls());
        assertEquals(1, metrics.scrapeCalls());
        assertEquals(1, metrics.scrapeFailures());

        metrics.setCurrentDayForTest(LocalDate.of(2026, 6, 22));
        metrics.incrementFmp();

        assertEquals(1, metrics.fmpCalls());
        assertEquals(0, metrics.scrapeCalls());
        assertEquals(0, metrics.scrapeFailures());
    }

    private List<String> logMessages() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private List<String> warnMessages() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
