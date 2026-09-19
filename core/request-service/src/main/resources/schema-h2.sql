CREATE TABLE IF NOT EXISTS requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created TIMESTAMP,
    status VARCHAR(255),
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL
);