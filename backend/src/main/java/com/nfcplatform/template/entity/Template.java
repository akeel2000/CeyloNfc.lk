package com.nfcplatform.template.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "templates")
public class Template extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "preview_image")
    private String previewImage;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor = "#4338ca";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateLayout layout = TemplateLayout.CLASSIC;

    @Column(nullable = false)
    private boolean premium = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
