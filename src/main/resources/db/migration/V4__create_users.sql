CREATE TABLE users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- A V3 declarou a FK como REFERENCES inline, que o InnoDB aceita e ignora.
-- Aqui ela é criada de verdade, no formato que o MySQL reconhece.
ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
