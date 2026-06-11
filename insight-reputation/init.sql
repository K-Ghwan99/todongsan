-- Insight Reputation Database Initialization
-- This file is used by Docker MySQL container to initialize the database

USE insight;

-- Grant privileges to insight_user
GRANT ALL PRIVILEGES ON insight.* TO 'insight_user'@'%';
FLUSH PRIVILEGES;

-- Set timezone
SET time_zone = '+09:00';