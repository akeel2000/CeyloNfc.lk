-- Public sales leads (contact form submissions). No auth required to create - the one
-- other deliberately public write endpoint besides account creation itself.
CREATE TABLE leads (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)   NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    phone         VARCHAR(50)   NULL,
    company       VARCHAR(255)  NULL,
    message       VARCHAR(2000) NULL,
    source        VARCHAR(100)  NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    notes         VARCHAR(2000) NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_leads_uuid UNIQUE (uuid),
    CONSTRAINT chk_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'CONVERTED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leads_status ON leads (status);

-- Support tickets are always tied to a client (B2B SaaS - there is no anonymous ticket
-- flow). One thread of messages per ticket, oldest first.
CREATE TABLE support_tickets (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)  NOT NULL,
    client_id     BIGINT       NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority      VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_support_tickets_uuid UNIQUE (uuid),
    CONSTRAINT fk_support_tickets_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT chk_support_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_support_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_support_tickets_client ON support_tickets (client_id);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);

CREATE TABLE support_messages (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid             VARCHAR(36)   NOT NULL,
    ticket_id        BIGINT        NOT NULL,
    sender_user_id   BIGINT        NOT NULL,
    body             VARCHAR(4000) NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_support_messages_uuid UNIQUE (uuid),
    CONSTRAINT fk_support_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_support_messages_ticket ON support_messages (ticket_id);

-- In-app notifications. Always addressed to a single user (never a role/broadcast row) -
-- fan-out on creation, one row per recipient, so unread counts stay a simple per-user count.
CREATE TABLE notifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(36)   NOT NULL,
    user_id       BIGINT        NOT NULL,
    type          VARCHAR(50)   NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    body          VARCHAR(1000) NULL,
    link          VARCHAR(500)  NULL,
    read_at       TIMESTAMP     NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notifications_uuid UNIQUE (uuid),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read_at);
