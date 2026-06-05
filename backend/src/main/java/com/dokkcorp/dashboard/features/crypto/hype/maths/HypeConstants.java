package com.dokkcorp.dashboard.features.crypto.hype.maths;

import java.math.BigDecimal;

public final class HypeConstants {

    public static final double TOTAL_SUPPLY = 1_000_000_000;
    public static final BigDecimal TOTAL_SUPPLY_BD = new BigDecimal("1000000000");
    public static final double FEE_RATE = 0.00022;
    public static final double STAKE_SCALE_FACTOR = 100_000_000d; // API validator stake uses 8 decimals

    private HypeConstants() {
    }
}
