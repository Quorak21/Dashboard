package com.dokkcorp.dashboard.features.assets.price;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class PriceProviderRegistry {

    private final Map<String, PriceProvider> providersById;

    public PriceProviderRegistry(List<PriceProvider> providers) {
        this.providersById = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalize(provider.providerId()),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException(
                                    "Duplicate price provider id: " + left.providerId());
                        }));
    }

    public Optional<PriceProvider> findById(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providersById.get(normalize(providerId)));
    }

    public PriceProvider requireById(String providerId) {
        return findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown price provider: " + providerId));
    }

    private static String normalize(String providerId) {
        return providerId.trim().toLowerCase(Locale.ROOT);
    }
}
