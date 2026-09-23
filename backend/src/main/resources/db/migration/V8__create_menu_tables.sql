-- One menu per client (auto-created on first access, same pattern as individual/company
-- profiles - see ProfileService/MenuService). Categories and items are ordered via
-- sort_order rather than relying on insertion order, so drag-reorder in the UI is a plain
-- integer update.

CREATE TABLE menus (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)   NOT NULL,
    client_id     BIGINT        NOT NULL,
    slug          VARCHAR(100)  NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    logo          VARCHAR(2048) NULL,
    description   VARCHAR(1000) NULL,
    currency      VARCHAR(10)   NOT NULL DEFAULT 'LKR',
    published     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_menus_uuid UNIQUE (uuid),
    CONSTRAINT uq_menus_client UNIQUE (client_id),
    CONSTRAINT uq_menus_slug UNIQUE (slug),
    CONSTRAINT fk_menus_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE menu_categories (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid         VARCHAR(36)   NOT NULL,
    menu_id      BIGINT        NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    sort_order   INT           NOT NULL DEFAULT 0,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_menu_categories_uuid UNIQUE (uuid),
    CONSTRAINT fk_menu_categories_menu FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_menu_categories_menu ON menu_categories (menu_id);

CREATE TABLE menu_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)    NOT NULL,
    category_id   BIGINT         NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    description   VARCHAR(1000)  NULL,
    image         VARCHAR(2048)  NULL,
    price         DECIMAL(10,2)  NOT NULL,
    available     BOOLEAN        NOT NULL DEFAULT TRUE,
    featured      BOOLEAN        NOT NULL DEFAULT FALSE,
    sort_order    INT            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_menu_items_uuid UNIQUE (uuid),
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES menu_categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_menu_items_category ON menu_items (category_id);
