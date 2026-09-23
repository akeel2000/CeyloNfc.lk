package com.nfcplatform.settings.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Single-row table - id is always 1, enforced in PlatformSettingsService. */
@Getter
@Setter
@Entity
@Table(name = "platform_settings")
public class PlatformSettings extends Auditable {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    @Column(name = "support_email", nullable = false)
    private String supportEmail;

    @Column(length = 255)
    private String tagline;
}
