-- Package plans (pricing tiers). Limits are enforced server-side (NfcCardService.assign
-- checks card_limit before allowing another card to be assigned) - never only a frontend
-- button-hiding concern, per docs/SECURITY.md.
CREATE TABLE package_plans (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                      VARCHAR(36)   NOT NULL,
    name                      VARCHAR(255)  NOT NULL,
    description               VARCHAR(1000) NULL,
    price                     DECIMAL(19,2) NOT NULL,
    billing_period            VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    card_limit                INT           NULL,
    profile_limit             INT           NULL,
    review_location_limit     INT           NULL,
    menu_limit                INT           NULL,
    premium_templates         BOOLEAN       NOT NULL DEFAULT FALSE,
    active                    BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order                INT           NOT NULL DEFAULT 0,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_package_plans_uuid UNIQUE (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subscriptions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid              VARCHAR(36)  NOT NULL,
    client_id         BIGINT       NOT NULL,
    package_plan_id   BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    start_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date          TIMESTAMP    NULL,
    renewal_date      TIMESTAMP    NULL,
    notes             VARCHAR(500) NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_subscriptions_uuid UNIQUE (uuid),
    CONSTRAINT uq_subscriptions_client UNIQUE (client_id),
    CONSTRAINT fk_subscriptions_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (package_plan_id) REFERENCES package_plans (id),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)   NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    sku           VARCHAR(100)  NOT NULL,
    description   VARCHAR(1000) NULL,
    price         DECIMAL(19,2) NOT NULL,
    image         VARCHAR(2048) NULL,
    type          VARCHAR(30)   NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_uuid UNIQUE (uuid),
    CONSTRAINT uq_products_sku UNIQUE (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid             VARCHAR(36)   NOT NULL,
    order_number     VARCHAR(30)   NOT NULL,
    client_id        BIGINT        NOT NULL,
    status           VARCHAR(30)   NOT NULL DEFAULT 'NEW',
    payment_status   VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    subtotal         DECIMAL(19,2) NOT NULL DEFAULT 0,
    total            DECIMAL(19,2) NOT NULL DEFAULT 0,
    notes            VARCHAR(1000) NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_orders_uuid UNIQUE (uuid),
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_client ON orders (client_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)   NOT NULL,
    order_id            BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            INT           NOT NULL DEFAULT 1,
    unit_price          DECIMAL(19,2) NOT NULL,
    customization_json  JSON          NULL,
    CONSTRAINT uq_order_items_uuid UNIQUE (uuid),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_items_order ON order_items (order_id);
