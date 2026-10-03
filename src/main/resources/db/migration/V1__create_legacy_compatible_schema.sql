CREATE TABLE IF NOT EXISTS trackingLinks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL DEFAULT '',
    timeGenerated INTEGER NOT NULL,
    emailUUID TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS linkVisits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timeVisited INTEGER NOT NULL,
    loggedIPAddress TEXT,
    sessionData TEXT NOT NULL DEFAULT '{}',
    emailUUID TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tracking_links_email_uuid
    ON trackingLinks (emailUUID);

CREATE INDEX IF NOT EXISTS idx_link_visits_email_uuid_time
    ON linkVisits (emailUUID, timeVisited DESC);

