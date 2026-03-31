CREATE TABLE players (
    id         BIGSERIAL   PRIMARY KEY,
    username   VARCHAR(64) NOT NULL UNIQUE,
    password   VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE games (
    id             BIGSERIAL   PRIMARY KEY,
    board          CHAR(9)     NOT NULL DEFAULT '_________',
    status         VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    player_x_id    BIGINT      NOT NULL REFERENCES players(id),
    player_o_id    BIGINT      REFERENCES players(id),
    next_player_id BIGINT      REFERENCES players(id),
    winner_id      BIGINT      REFERENCES players(id),
    version        INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
