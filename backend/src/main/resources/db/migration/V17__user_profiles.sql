CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    biography VARCHAR(500),
    time_zone VARCHAR(64),
    photo_key VARCHAR(80)
);
