package com.flipkart.marketing.connekt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author durga.s
 * @version 1/19/16
 */
public class Utils {
    public static byte[] shaHash(byte[] toHash) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(toHash);
    }
}
