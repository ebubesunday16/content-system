-- SEO Content Generator - Database Schema
-- PostgreSQL 12+

-- Create database
CREATE DATABASE seo_content_db;

-- Create user
CREATE USER seo_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE seo_content_db TO seo_user;

-- Connect to database
\c seo_content_db;

-- Tables will be auto-created by Hibernate, but here's the schema for reference:

-- =====================================================
-- NICHES TABLE
-- =====================================================
CREATE TABLE niches (
    id BIGSERIAL PRIMARY KEY,
    niche_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    seed_keywords TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_niche_name ON niches(niche_name);

-- =====================================================
-- POTENTIAL_KEYWORDS TABLE
-- =====================================================
CREATE TABLE potential_keywords (
    id BIGSERIAL PRIMARY KEY,
    keyword_text VARCHAR(255) NOT NULL UNIQUE,
    depth_level INTEGER NOT NULL,
    parent_keyword_id BIGINT REFERENCES potential_keywords(id) ON DELETE SET NULL,
    niche_id BIGINT NOT NULL REFERENCES niches(id) ON DELETE CASCADE,
    qualification_score DOUBLE PRECISION,
    qualification_reasoning TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'UNWRITTEN',
    discovered_date TIMESTAMP NOT NULL DEFAULT NOW(),
    written_date TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('UNWRITTEN', 'WRITTEN', 'REJECTED'))
);

CREATE INDEX idx_keyword_text ON potential_keywords(keyword_text);
CREATE INDEX idx_status ON potential_keywords(status);
CREATE INDEX idx_depth_level ON potential_keywords(depth_level);
CREATE INDEX idx_qualification_score ON potential_keywords(qualification_score);
CREATE INDEX idx_niche_id ON potential_keywords(niche_id);
CREATE INDEX idx_parent_keyword_id ON potential_keywords(parent_keyword_id);

-- =====================================================
-- ARTICLES TABLE
-- =====================================================
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    keyword_id BIGINT NOT NULL UNIQUE REFERENCES potential_keywords(id) ON DELETE CASCADE,
    niche_id BIGINT NOT NULL REFERENCES niches(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    meta_description VARCHAR(500),
    content TEXT NOT NULL,
    word_count INTEGER,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    published_date TIMESTAMP
);

CREATE INDEX idx_created_date ON articles(created_date);
CREATE INDEX idx_published_date ON articles(published_date);
CREATE INDEX idx_article_niche_id ON articles(niche_id);
CREATE INDEX idx_keyword_id ON articles(keyword_id);

-- =====================================================
-- EXPLORATION_LOGS TABLE
-- =====================================================
CREATE TABLE exploration_logs (
    id BIGSERIAL PRIMARY KEY,
    niche_id BIGINT NOT NULL REFERENCES niches(id) ON DELETE CASCADE,
    execution_date TIMESTAMP NOT NULL DEFAULT NOW(),
    exploration_strategy TEXT,
    current_max_depth_level INTEGER,
    keywords_discovered INTEGER DEFAULT 0,
    keywords_qualified INTEGER DEFAULT 0,
    articles_generated INTEGER DEFAULT 0,
    llm_notes TEXT,
    execution_duration_ms BIGINT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT
);

CREATE INDEX idx_execution_date ON exploration_logs(execution_date);
CREATE INDEX idx_log_niche_id ON exploration_logs(niche_id);

-- =====================================================
-- USEFUL QUERIES
-- =====================================================

-- Get niche overview
SELECT 
    n.niche_name,
    COUNT(DISTINCT pk.id) as total_keywords,
    COUNT(DISTINCT CASE WHEN pk.status = 'UNWRITTEN' THEN pk.id END) as unwritten,
    COUNT(DISTINCT CASE WHEN pk.status = 'WRITTEN' THEN pk.id END) as written,
    COUNT(DISTINCT a.id) as total_articles,
    MAX(pk.depth_level) as max_depth
FROM niches n
LEFT JOIN potential_keywords pk ON n.id = pk.niche_id
LEFT JOIN articles a ON n.id = a.niche_id
GROUP BY n.id, n.niche_name;

-- Get keyword tree structure
SELECT 
    pk.depth_level,
    pk.keyword_text,
    pk.qualification_score,
    pk.status,
    parent.keyword_text as parent_keyword
FROM potential_keywords pk
LEFT JOIN potential_keywords parent ON pk.parent_keyword_id = parent.id
WHERE pk.niche_id = 1
ORDER BY pk.depth_level, pk.qualification_score DESC;

-- Get top performing keywords (not yet written)
SELECT 
    keyword_text,
    qualification_score,
    depth_level,
    qualification_reasoning
FROM potential_keywords
WHERE niche_id = 1 
  AND status = 'UNWRITTEN'
  AND qualification_score >= 7.0
ORDER BY qualification_score DESC
LIMIT 20;

-- Get execution history
SELECT 
    execution_date,
    keywords_discovered,
    keywords_qualified,
    articles_generated,
    execution_duration_ms / 1000.0 as duration_seconds,
    success
FROM exploration_logs
WHERE niche_id = 1
ORDER BY execution_date DESC
LIMIT 10;

-- Get articles with keywords
SELECT 
    a.title,
    pk.keyword_text,
    a.word_count,
    a.created_date
FROM articles a
JOIN potential_keywords pk ON a.keyword_id = pk.id
WHERE a.niche_id = 1
ORDER BY a.created_date DESC;

-- Analyze keyword depth distribution
SELECT 
    depth_level,
    COUNT(*) as keyword_count,
    AVG(qualification_score) as avg_score,
    COUNT(CASE WHEN status = 'WRITTEN' THEN 1 END) as written_count
FROM potential_keywords
WHERE niche_id = 1
GROUP BY depth_level
ORDER BY depth_level;

-- Find orphaned keywords (no parent but depth > 0)
SELECT 
    keyword_text,
    depth_level,
    qualification_score
FROM potential_keywords
WHERE parent_keyword_id IS NULL 
  AND depth_level > 0
  AND niche_id = 1;
