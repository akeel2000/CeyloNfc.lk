package com.nfcplatform.lead.dto;

import com.nfcplatform.client.dto.ClientCreateResponse;

public record LeadConvertResponse(
        LeadResponse lead,
        ClientCreateResponse client
) {
}
