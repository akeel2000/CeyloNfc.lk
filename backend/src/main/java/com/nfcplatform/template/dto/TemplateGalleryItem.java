package com.nfcplatform.template.dto;

/** locked = premium template the caller's current plan doesn't include - selectable in the UI but should show a lock/upgrade badge, not hidden entirely. */
public record TemplateGalleryItem(TemplateResponse template, boolean locked, boolean selected) {
}
