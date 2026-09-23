package com.nfcplatform.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugify {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private Slugify() {
    }

    public static String of(String input) {
        String base = input == null ? "" : input.toLowerCase(Locale.ROOT);
        base = NON_ALPHANUMERIC.matcher(base).replaceAll("-");
        base = base.replaceAll("^-+|-+$", "");
        return base.isBlank() ? "profile" : base;
    }
}
