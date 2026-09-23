package com.nfcplatform.menu.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "menus")
public class Menu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false, unique = true)
    private Long clientId;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String logo;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 10)
    private String currency = "LKR";

    @Column(nullable = false)
    private boolean published = false;
}
