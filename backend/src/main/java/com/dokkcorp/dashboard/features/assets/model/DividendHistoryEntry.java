package com.dokkcorp.dashboard.features.assets.model;

import java.math.BigDecimal;

public record DividendHistoryEntry(int year, BigDecimal amount, String currency) {}
