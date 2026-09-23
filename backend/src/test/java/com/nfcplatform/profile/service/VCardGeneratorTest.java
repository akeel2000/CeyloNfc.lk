package com.nfcplatform.profile.service;

import com.nfcplatform.profile.dto.PublicProfileResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VCardGeneratorTest {

    @Test
    void generatesAWellFormedVCardForAnIndividualWithACompany() {
        PublicProfileResponse profile = new PublicProfileResponse("INDIVIDUAL", "jane-doe", "Jane Doe",
                "Engineer", "Acme Ltd", null, null, null, null, null, "+94770000000", null,
                "jane@example.com", "https://example.com", "123 Main St", "Colombo", "Sri Lanka",
                null, null, null, List.of(), null, null, null);

        String vcard = VCardGenerator.generate(profile);

        assertThat(vcard).startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        assertThat(vcard).endsWith("END:VCARD\r\n");
        assertThat(vcard).contains("FN:Jane Doe\r\n");
        assertThat(vcard).contains("ORG:Acme Ltd\r\n");
        assertThat(vcard).contains("TITLE:Engineer\r\n");
        assertThat(vcard).contains("TEL;TYPE=CELL:+94770000000\r\n");
        assertThat(vcard).contains("EMAIL:jane@example.com\r\n");
    }

    @Test
    void usesTheCompanyNameAsTheDisplayNameForABusinessProfile() {
        PublicProfileResponse profile = new PublicProfileResponse("BUSINESS", "acme", null, null, "Acme Ltd",
                "Retail", null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), null, null, null);

        String vcard = VCardGenerator.generate(profile);

        assertThat(vcard).contains("FN:Acme Ltd\r\n");
        // A business profile's own name never gets duplicated into ORG - only an individual's
        // affiliated company does.
        assertThat(vcard).doesNotContain("ORG:");
    }

    @Test
    void omitsOptionalFieldsEntirelyWhenTheyAreNotSet() {
        PublicProfileResponse profile = new PublicProfileResponse("INDIVIDUAL", "jane-doe", "Jane Doe",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of(), null, null, null);

        String vcard = VCardGenerator.generate(profile);

        assertThat(vcard).doesNotContain("TITLE:").doesNotContain("TEL;").doesNotContain("EMAIL:")
                .doesNotContain("URL:").doesNotContain("ADR;");
    }

    @Test
    void escapesCommasSemicolonsAndBackslashesPerTheVCardSpec() {
        PublicProfileResponse profile = new PublicProfileResponse("INDIVIDUAL", "jane-doe",
                "Jane; \"Semicolon\", Backslash\\ Doe", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, List.of(), null, null, null);

        String vcard = VCardGenerator.generate(profile);

        assertThat(vcard).contains("FN:Jane\\; \"Semicolon\"\\, Backslash\\\\ Doe\r\n");
    }

    @Test
    void buildsTheAddressFieldFromAddressCityAndCountry() {
        PublicProfileResponse profile = new PublicProfileResponse("INDIVIDUAL", "jane-doe", "Jane Doe",
                null, null, null, null, null, null, null, null, null, null, null,
                "123 Main St", "Colombo", "Sri Lanka", null, null, null, List.of(), null, null, null);

        String vcard = VCardGenerator.generate(profile);

        assertThat(vcard).contains("ADR;TYPE=WORK:;;123 Main St;Colombo;;;Sri Lanka\r\n");
    }
}
