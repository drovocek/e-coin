package com.company.ecoin.util;

import lombok.SneakyThrows;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
    public static PublicKey generateDsaKey(byte[] data) {
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(data);
        return DSA_KEY_FACTORY.generatePublic(pubSpec);
    }
}
