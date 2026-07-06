package com.dokkcorp.dashboard.features.assets;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderCallMetrics {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCallMetrics.class);
    private static final double SCRAPE_FAILURE_WARN_THRESHOLD_PERCENT = 20.0;

    @Value("${app.alerts.fmp-calls-warn-threshold:200}")
    private int fmpWarnThreshold;

    private final Clock clock;
    private final Object dayResetLock = new Object();
    private final AtomicLong fmpCalls = new AtomicLong(0);
    private final AtomicLong scrapeCalls = new AtomicLong(0);
    private final AtomicLong scrapeFailures = new AtomicLong(0);
    private volatile LocalDate currentDay;

    public ProviderCallMetrics() {
        this(Clock.systemUTC());
    }

    private ProviderCallMetrics(Clock clock) {
        this.clock = clock;
        this.currentDay = LocalDate.now(clock);
    }

    private ProviderCallMetrics(int fmpWarnThreshold, Clock clock) {
        this.fmpWarnThreshold = fmpWarnThreshold;
        this.clock = clock;
        this.currentDay = LocalDate.now(clock);
    }

    static ProviderCallMetrics createForTest(int fmpWarnThreshold, Clock clock) {
        return new ProviderCallMetrics(fmpWarnThreshold, clock);
    }

    public void incrementFmp() {
        synchronized (dayResetLock) {
            resetDayIfNeeded();
            fmpCalls.incrementAndGet();
        }
    }

    public void incrementScrape() {
        synchronized (dayResetLock) {
            resetDayIfNeeded();
            scrapeCalls.incrementAndGet();
        }
    }

    public void recordScrapeFailure() {
        synchronized (dayResetLock) {
            resetDayIfNeeded();
            scrapeFailures.incrementAndGet();
        }
    }

    public void logMetrics() {
        long fmp;
        long scrape;
        long scrapeFails;
        synchronized (dayResetLock) {
            resetDayIfNeeded();
            fmp = fmpCalls.get();
            scrape = scrapeCalls.get();
            scrapeFails = scrapeFailures.get();
        }

        logger.info("FMP calls today: {}", fmp);
        logger.info("Scrape calls today: {}", scrape);

        if (fmp > fmpWarnThreshold) {
            logger.warn("⚠ FMP calls today: {} — approaching daily limit", fmp);
        }

        if (scrape > 0) {
            double failRate = (double) scrapeFails / scrape * 100;
            if (failRate > SCRAPE_FAILURE_WARN_THRESHOLD_PERCENT) {
                logger.warn("⚠ Scrape failure rate: {}% — above 20% threshold",
                        String.format("%.1f", failRate));
            }
        }
    }

    private void resetDayIfNeeded() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(currentDay)) {
            fmpCalls.set(0);
            scrapeCalls.set(0);
            scrapeFailures.set(0);
            currentDay = today;
        }
    }

    long fmpCalls() {
        return fmpCalls.get();
    }

    long scrapeCalls() {
        return scrapeCalls.get();
    }

    long scrapeFailures() {
        return scrapeFailures.get();
    }

    LocalDate currentDay() {
        return currentDay;
    }

    void setCurrentDayForTest(LocalDate day) {
        this.currentDay = day;
    }
}
