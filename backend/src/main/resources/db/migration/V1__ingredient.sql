CREATE TABLE ingredient (
    id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120)   NOT NULL,
    category    VARCHAR(30)    NOT NULL,
    quantity    DECIMAL(10, 3) NOT NULL,
    unit        VARCHAR(10)    NOT NULL,
    expiry_date DATE           NULL,
    added_at    TIMESTAMP(6)   NOT NULL
);
