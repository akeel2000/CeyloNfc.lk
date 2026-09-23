package com.nfcplatform.client.dto;

/**
 * Returned only once, immediately after creation. temporaryPassword is shown so the
 * admin can hand it to the client if the welcome email doesn't reach them (e.g. no SMTP
 * configured in dev) - it is never recoverable from the API again afterwards.
 */
public record ClientCreateResponse(
        ClientResponse client,
        String temporaryPassword
) {
}
