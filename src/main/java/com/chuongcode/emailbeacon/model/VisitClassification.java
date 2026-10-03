package com.chuongcode.emailbeacon.model;

public enum VisitClassification {
    HUMAN_LIKELY,
    MAIL_PROXY,
    AUTOMATED,
    TEST,
    UNKNOWN;

    public boolean countsAsOpen() {
        return this != AUTOMATED && this != TEST;
    }
}

