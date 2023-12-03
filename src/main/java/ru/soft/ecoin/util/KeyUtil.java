package ru.soft.ecoin.util;

import lombok.SneakyThrows;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class KeyUtil {

    private static final KeyFactory DSA_KEY_FACTORY;

    static {
        try {
            DSA_KEY_FACTORY = KeyFactory.getInstance("DSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static PublicKey generateDsaPublicKey(byte[] data) {
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(data);
        return DSA_KEY_FACTORY.generatePublic(pubSpec);
    }

    @SneakyThrows
    public static PrivateKey generateDsaPrivateKey(byte[] data) {
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(data);
        return DSA_KEY_FACTORY.generatePrivate(privSpec);
    }
}
