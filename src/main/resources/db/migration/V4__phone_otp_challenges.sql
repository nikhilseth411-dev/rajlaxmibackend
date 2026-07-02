CREATE TABLE phone_otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(10) NOT NULL,
    provider_request_id VARCHAR(200) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_phone_otp_phone ON phone_otp_challenges(phone);
CREATE INDEX idx_phone_otp_created ON phone_otp_challenges(created_at);
