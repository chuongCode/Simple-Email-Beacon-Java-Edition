package com.chuongcode.emailbeacon.web;

import java.util.Base64;

final class TrackingPixel {

    static final byte[] GIF_BYTES = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==");

    private TrackingPixel() {
    }
}

