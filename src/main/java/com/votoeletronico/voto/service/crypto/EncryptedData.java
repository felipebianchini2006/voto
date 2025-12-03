package com.votoeletronico.voto.service.crypto;

/**
 * Container for encrypted data with metadata
 */
public record EncryptedData(
        String ciphertext,
        String nonce,
        String algorithm
) {
}
