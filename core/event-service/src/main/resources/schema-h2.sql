CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_name ON categories (name);

CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation VARCHAR(2000) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    description VARCHAR(7000) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    location CLOB NOT NULL,
    paid BOOLEAN NOT NULL,
    participant_limit INT NOT NULL,
    published_on TIMESTAMP,
    request_moderation BOOLEAN NOT NULL,
    state VARCHAR(255) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,
    rate BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_events_categories FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_events_state CHECK (state IN ('PENDING','PUBLISHED','CANCELED'))
);

CREATE TABLE IF NOT EXISTS compilations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pinned BOOLEAN NOT NULL,
    title VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT fk_compilation_events_compilations FOREIGN KEY (compilation_id) REFERENCES compilations(id) ON DELETE CASCADE,
    CONSTRAINT fk_compilation_events_events FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);