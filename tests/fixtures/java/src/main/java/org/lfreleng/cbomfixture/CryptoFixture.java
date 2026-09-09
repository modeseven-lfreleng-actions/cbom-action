// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 The Linux Foundation

package org.lfreleng.cbomfixture;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Fixture exercising JCA entry points the sonar-cryptography plugin
 * detects. Not production code: it exists so the CBOM scan has a known,
 * stable set of cryptographic assets to find.
 */
public final class CryptoFixture {

    private CryptoFixture() {}

    public static byte[] digest(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(input);
    }

    public static SecretKey aesKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    public static Cipher aesCipher() throws Exception {
        return Cipher.getInstance("AES/CBC/PKCS5Padding");
    }
}
