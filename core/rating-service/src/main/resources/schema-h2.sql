CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    reaction VARCHAR(10) NOT NULL CHECK (reaction IN ('LIKE', 'DISLIKE')),
    CONSTRAINT uq_ratings_user_event UNIQUE (user_id, event_id)
);