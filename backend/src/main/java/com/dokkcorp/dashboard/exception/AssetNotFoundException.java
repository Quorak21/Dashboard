package com.dokkcorp.dashboard.exception;

/**
 * Exception levée lorsqu'un actif demandé n'est pas présent dans le registre.
 */
public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String assetId) {
        super("Unknown asset: " + assetId);
    }
}
