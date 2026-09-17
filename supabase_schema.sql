-- ==========================================================
-- GAPOO (گپو) - Free Cloud Database Schema (Supabase / PostgreSQL + PostGIS)
-- Run this in Supabase SQL Editor (https://supabase.com/dashboard/project/_/sql)
-- ==========================================================

-- 1. Enable PostGIS extension for spatial queries (Section 16.3 of Spec)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. GROUPS TABLE (Social Interest Groups)
CREATE TABLE IF NOT EXISTS public.groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,          -- علمی، هنری، فرهنگی، ورزشی، سرگرمی، سلامت
    sub_category TEXT DEFAULT '',
    description TEXT DEFAULT '',
    location_name TEXT DEFAULT '',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    member_count INT DEFAULT 1,
    max_capacity INT DEFAULT 20,
    schedule_type TEXT DEFAULT 'هفتگی',
    group_type TEXT DEFAULT 'PERSISTENT', -- INSTANT, PLANNED, PERSISTENT
    is_active BOOLEAN DEFAULT TRUE,
    icon_emoji TEXT DEFAULT '👥',
    creator_name TEXT DEFAULT 'کاربر گپو',
    creator_trust_score INT DEFAULT 85,
    rules TEXT DEFAULT 'احترام به اعضا در مکان عمومی',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Spatial index on groups location
CREATE INDEX IF NOT EXISTS idx_groups_coords ON public.groups (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_groups_category ON public.groups (category, is_active);

-- 3. EVENTS TABLE
CREATE TABLE IF NOT EXISTS public.events (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    event_type TEXT NOT NULL,        -- کنسرت، نمایش فیلم، کارگاه، جشن، تئاتر
    description TEXT DEFAULT '',
    organizer_name TEXT DEFAULT '',
    organizer_trust_score INT DEFAULT 90,
    location_name TEXT DEFAULT '',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    price DOUBLE PRECISION DEFAULT 0.0,
    capacity INT DEFAULT 30,
    registered_count INT DEFAULT 0,
    start_time BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    poster_emoji TEXT DEFAULT '🎟️',
    has_companion_request BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_coords ON public.events (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_events_type ON public.events (event_type, is_active);

-- 4. MOOD PULSES TABLE (Instant Meetup Pulses - 30 min lifetime)
CREATE TABLE IF NOT EXISTS public.mood_pulses (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL,
    user_avatar_emoji TEXT DEFAULT '😊',
    mood_type TEXT NOT NULL,         -- chat, walk, tea, book, group_activity
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status_message TEXT DEFAULT '',
    trust_score INT DEFAULT 80,
    expires_at BIGINT DEFAULT ((extract(epoch from now()) + 1800) * 1000)::bigint,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mood_pulses_coords ON public.mood_pulses (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_mood_pulses_active ON public.mood_pulses (mood_type, is_active);

-- 5. Row Level Security (RLS) Configuration for Pilot Testing
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mood_pulses ENABLE ROW LEVEL SECURITY;

-- Allow public anonymous read/write for free testing (Can be restricted later)
CREATE POLICY "Allow public read groups" ON public.groups FOR SELECT USING (true);
CREATE POLICY "Allow public insert groups" ON public.groups FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public read events" ON public.events FOR SELECT USING (true);
CREATE POLICY "Allow public insert events" ON public.events FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public read mood_pulses" ON public.mood_pulses FOR SELECT USING (true);
CREATE POLICY "Allow public insert mood_pulses" ON public.mood_pulses FOR INSERT WITH CHECK (true);
