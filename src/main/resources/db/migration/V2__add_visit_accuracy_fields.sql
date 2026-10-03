ALTER TABLE linkVisits ADD COLUMN userAgent TEXT;
ALTER TABLE linkVisits ADD COLUMN classification TEXT NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE linkVisits ADD COLUMN visitorHash TEXT;
ALTER TABLE linkVisits ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0;
ALTER TABLE linkVisits ADD COLUMN testVisit INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_link_visits_visitor_window
    ON linkVisits (emailUUID, visitorHash, timeVisited DESC);

