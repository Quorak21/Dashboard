package com.dokkcorp.dashboard.exception;

/**
 * Échec d'un appel HTTP vers une API tierce (après retry).
 * Les services métier attrapent cette exception pour cache / DTO d'erreur.
 */
public class ExternalProviderException extends RuntimeException {

    public ExternalProviderException(String provider, String message, Throwable cause) {
        super("[%s] %s".formatted(provider, message), cause);
    }

    public ExternalProviderException(String provider, String message) {
        super("[%s] %s".formatted(provider, message));
    }
}
