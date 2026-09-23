package com.nfcplatform.destination.entity;

public enum DestinationType {
    // Resolve to a stored external_url.
    WEBSITE,
    WHATSAPP,
    SOCIAL,
    CUSTOM_URL,

    // Resolved dynamically at redirect time from the client's own profile/menu/review location
    // (see DestinationResolverService) rather than a stored external_url - no destination edit
    // needed when the underlying profile/menu/review location changes.
    PROFILE,
    COMPANY_PROFILE,
    GOOGLE_REVIEW,
    MENU,

    // Resolves dynamically to the published profile's downloadable vCard.
    VCARD;

    public boolean isUrlBased() {
        return this == WEBSITE || this == WHATSAPP || this == SOCIAL || this == CUSTOM_URL;
    }
}
