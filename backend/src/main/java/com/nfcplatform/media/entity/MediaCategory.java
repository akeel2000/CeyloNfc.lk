package com.nfcplatform.media.entity;

/**
 * Closed set of upload targets. Deliberately an enum rather than a client-supplied folder
 * name string - the category becomes part of a filesystem path in LocalStorageService, so an
 * open string here would be a path-traversal vector.
 */
public enum MediaCategory {
    PROFILE_PHOTO("profile"),
    PROFILE_COVER("profile"),
    COMPANY_LOGO("profile"),
    MENU_ITEM("menu"),
    TICKET_ATTACHMENT("tickets"),
    TEMPLATE_PREVIEW("templates");

    private final String folder;

    MediaCategory(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
