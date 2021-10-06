package org.mifos.connector.slcb.utils;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class SecurityUtils {

    public static String signContent(String content, String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey.getBytes(StandardCharsets.UTF_8))));

        java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));

        byte[] signed = signature.sign();
        return Base64.encodeBase64String(signed);
    }

}
