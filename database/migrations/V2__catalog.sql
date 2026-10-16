-- =====================================================================
-- V2  Catalogue: stores, products, favourites      owner: Member 2
-- =====================================================================

CREATE TABLE stores (
    id           VARCHAR(10)  PRIMARY KEY,                   -- R1, S1, P1 … (prefix = type)
    type         VARCHAR(12)  NOT NULL
                 CHECK (type IN ('FOOD', 'GROCERY', 'PHARMACY', 'CLOTHES', 'TECH', 'OTHER')),
    cuisine      VARCHAR(20)  NOT NULL,                      -- category shown to customers
    name         VARCHAR(80)  NOT NULL,
    name_ar      VARCHAR(80)  NOT NULL,
    tagline      VARCHAR(120),
    tagline_ar   VARCHAR(120),
    phone        VARCHAR(20),
    address      VARCHAR(300),
    photo_url    VARCHAR(300),
    opens        TIME,                                       -- null = no published hours
    closes       TIME,
    accepting    BOOLEAN      NOT NULL DEFAULT TRUE,         -- the store's own open/closed switch
    lat          DOUBLE PRECISION,
    lng          DOUBLE PRECISION,
    commission   NUMERIC(5, 2) NOT NULL DEFAULT 10.00
                 CHECK (commission BETWEEN 0 AND 50),        -- % the platform keeps
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD CONSTRAINT users_store FOREIGN KEY (store_id) REFERENCES stores (id);

CREATE TABLE products (
    id           VARCHAR(10)    PRIMARY KEY,
    store_id     VARCHAR(10)    NOT NULL REFERENCES stores (id),
    name         VARCHAR(120)   NOT NULL,
    name_ar      VARCHAR(120)   NOT NULL,
    description  VARCHAR(500),
    price        NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    size         VARCHAR(10)    NOT NULL DEFAULT 'MEDIUM'
                 CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE')), -- delivery weight (LARGE → tuk-tuk)
    section      VARCHAR(20)    NOT NULL,                     -- menu section
    photo_url    VARCHAR(300),
    available    BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order   INT            NOT NULL DEFAULT 0
);
CREATE INDEX products_store ON products (store_id);
CREATE INDEX products_name ON products (lower(name));

CREATE TABLE favorites (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    fav_key VARCHAR(20) NOT NULL,                             -- store:R1 / product:F3
    PRIMARY KEY (user_id, fav_key)
);
