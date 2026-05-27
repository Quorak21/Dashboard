package com.dokkcorp.dashboard.features.crypto.hype;

import java.math.BigDecimal;

public final class HypeConstants {

    public static final double MAX_SUPPLY = 1_000_000_000;
    public static final BigDecimal MAX_SUPPLY_BD = new BigDecimal("1000000000"); // TODO: A check si vraiment utile ou si on passe tout en BD ou en double
    public static final double FEE_RATE = 0.00022;

    private HypeConstants() {
    }
}
