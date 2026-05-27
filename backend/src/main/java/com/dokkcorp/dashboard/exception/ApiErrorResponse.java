package com.dokkcorp.dashboard.exception;

// Corps JSON renvoyé par GlobalExceptionHandler en cas d'erreur HTTP. Simple et basique pour le moment
public record ApiErrorResponse(String message) {
}
