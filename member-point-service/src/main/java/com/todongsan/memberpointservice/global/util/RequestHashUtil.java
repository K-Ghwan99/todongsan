package com.todongsan.memberpointservice.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RequestHashUtil {

    private RequestHashUtil() {}

    public static String compute(Long memberId, String type, BigDecimal amount,
                                 String referenceType, Long referenceId) {
        String normalizedAmount = amount.setScale(2, RoundingMode.DOWN).toPlainString();
        String raw = memberId + "|" + type + "|" + normalizedAmount + "|" + referenceType + "|" + referenceId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
