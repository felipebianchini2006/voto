package com.votoeletronico.voto.service.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for cryptographic operations
 * Handles encryption, decryption, signing, and hashing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_NONCE_LENGTH = 12;

    static {
        // Add Bouncy Castle as security provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate RSA key pair for election
     */
    public KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            log.info("Generated RSA key pair");
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate RSA key pair", e);
            throw new CryptoException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Generate AES key for symmetric encryption
     */
    public SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            log.debug("Generated AES key");
            return key;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate AES key", e);
            throw new CryptoException("Failed to generate AES key", e);
        }
    }

    /**
     * Encrypt data with AES-GCM
     */
    public EncryptedData encryptAES(String plaintext, SecretKey key) {
        try {
            byte[] nonce = generateNonce(GCM_NONCE_LENGTH);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String encryptedText = Base64.getEncoder().encodeToString(ciphertext);
            String nonceBase64 = Base64.getEncoder().encodeToString(nonce);

            log.debug("Encrypted data with AES-GCM");
            return new EncryptedData(encryptedText, nonceBase64, AES_TRANSFORMATION);
        } catch (Exception e) {
            log.error("Failed to encrypt data with AES", e);
            throw new CryptoException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt data with AES-GCM
     */
    public String decryptAES(String ciphertext, String nonceBase64, SecretKey key) {
        try {
            byte[] nonce = Base64.getDecoder().decode(nonceBase64);
            byte[] encryptedBytes = Base64.getDecoder().decode(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(encryptedBytes);

            log.debug("Decrypted data with AES-GCM");
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data with AES", e);
            throw new CryptoException("Failed to decrypt data", e);
        }
    }

    /**
     * Encrypt data with RSA public key (for small data like AES keys)
     */
    public String encryptRSA(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(data);
            log.debug("Encrypted data with RSA");
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt data with RSA", e);
            throw new CryptoException("Failed to encrypt data with RSA", e);
        }
    }

    /**
     * Decrypt data with RSA private key
     */
    public byte[] decryptRSA(String encryptedData, PrivateKey privateKey) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedData);
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(encrypted);
            log.debug("Decrypted data with RSA");
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt data with RSA", e);
            throw new CryptoException("Failed to decrypt data with RSA", e);
        }
    }

    /**
     * Sign data with private key
     */
    public String signData(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            log.debug("Signed data");
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to sign data", e);
            throw new CryptoException("Failed to sign data", e);
        }
    }

    /**
     * Verify signature with public key
     */
    public boolean verifySignature(String data, String signatureBase64, PublicKey publicKey) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            boolean verified = signature.verify(signatureBytes);
            log.debug("Verified signature: {}", verified);
            return verified;
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }

    /**
     * Hash data with SHA-256
     */
    public String hashSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash data", e);
            throw new CryptoException("Failed to hash data", e);
        }
    }

    /**
     * Generate secure random nonce
     */
    public byte[] generateNonce(int length) {
        byte[] nonce = new byte[length];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    /**
     * Generate secure random token
     */
    public String generateSecureToken() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Generate unique nonce string
     */
    public String generateNonceString() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    /**
     * Encode public key to Base64
     */
    public String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Encode private key to Base64
     */
    public String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Decode public key from Base64
     */
    public PublicKey decodePublicKey(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            log.error("Failed to decode public key", e);
            throw new CryptoException("Failed to decode public key", e);
        }
    }

    /**
     * Decode private key from Base64
     */
    public PrivateKey decodePrivateKey(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            log.error("Failed to decode private key", e);
            throw new CryptoException("Failed to decode private key", e);
        }
    }

    /**
     * Encode AES key to Base64
     */
    public String encodeAESKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Decode AES key from Base64
     */
    public SecretKey decodeAESKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Create hash chain entry
     */
    public String createChainHash(String currentData, String previousHash) {
        String combined = (previousHash != null ? previousHash : "") + currentData;
        return hashSHA256(combined);
    }
}
