package com.nfcplatform.profile.service;

import com.nfcplatform.profile.dto.PublicProfileResponse;

public final class VCardGenerator {

    private VCardGenerator() {
    }

    public static String generate(PublicProfileResponse profile) {
        String name = profile.type().equals("INDIVIDUAL") ? profile.fullName() : profile.companyName();
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\n").append("VERSION:3.0\r\n");
        sb.append("N:").append(escape(name)).append("\r\n");
        sb.append("FN:").append(escape(name)).append("\r\n");
        if (profile.companyName() != null && profile.type().equals("INDIVIDUAL")) {
            sb.append("ORG:").append(escape(profile.companyName())).append("\r\n");
        }
        if (profile.jobTitle() != null) {
            sb.append("TITLE:").append(escape(profile.jobTitle())).append("\r\n");
        }
        if (profile.phone() != null) {
            sb.append("TEL;TYPE=CELL:").append(escape(profile.phone())).append("\r\n");
        }
        if (profile.email() != null) {
            sb.append("EMAIL:").append(escape(profile.email())).append("\r\n");
        }
        if (profile.website() != null) {
            sb.append("URL:").append(escape(profile.website())).append("\r\n");
        }
        if (profile.address() != null) {
            sb.append("ADR;TYPE=WORK:;;").append(escape(profile.address())).append(";")
                    .append(escape(profile.city())).append(";;;").append(escape(profile.country())).append("\r\n");
        }
        sb.append("END:VCARD\r\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
