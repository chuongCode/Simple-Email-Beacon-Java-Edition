package com.chuongcode.emailbeacon.service;

import com.chuongcode.emailbeacon.config.BeaconProperties;
import com.chuongcode.emailbeacon.model.VisitClassification;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class VisitClassifier {

    private static final String[] MAIL_PROXY_MARKERS = {
            "googleimageproxy",
            "yahoomailproxy",
            "mail.ru_image_proxy",
            "protection.outlook"
    };

    private static final String[] AUTOMATED_MARKERS = {
            "bot",
            "crawler",
            "spider",
            "scanner",
            "preview",
            "headless",
            "curl/",
            "wget/",
            "python-requests",
            "java-http-client",
            "facebookexternalhit",
            "slackbot"
    };

    private final byte[] hashKey;

    public VisitClassifier(BeaconProperties properties) {
        this.hashKey = properties.visitorHashSalt().getBytes(StandardCharsets.UTF_8);
    }

    public VisitClassification classify(String userAgent, boolean testVisit) {
        if (testVisit) {
            return VisitClassification.TEST;
        }
        if (userAgent == null || userAgent.isBlank()) {
            return VisitClassification.UNKNOWN;
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, MAIL_PROXY_MARKERS)) {
            return VisitClassification.MAIL_PROXY;
        }
        if (containsAny(normalized, AUTOMATED_MARKERS)) {
            return VisitClassification.AUTOMATED;
        }
        return VisitClassification.HUMAN_LIKELY;
    }

    public String visitorHash(String ipAddress, String userAgent) {
        String fingerprint = valueOrEmpty(ipAddress) + '\0' + valueOrEmpty(userAgent);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(fingerprint.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create visitor hash", exception);
        }
    }

    private boolean containsAny(String value, String[] markers) {
        for (String marker : markers) {
            if (value.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}

