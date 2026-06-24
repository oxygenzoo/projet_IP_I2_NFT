$$ LANGUAGE plpgsql;

-- Tables
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    last_login TIMESTAMP,
    consent_rgpd BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP
);

CREATE TABLE travels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    destination VARCHAR(200),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    travel_id UUID REFERENCES travels(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    storage_url TEXT NOT NULL,
    format VARCHAR(20),
    capture_date TIMESTAMP,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    width INT,
    height INT,
    size_mb NUMERIC(8,2),
    imported_at TIMESTAMP DEFAULT NOW(),
    is_duplicate BOOLEAN DEFAULT FALSE,
    duplicate_of UUID REFERENCES photos(id) ON DELETE SET NULL,
    brightness_score NUMERIC(5,2),
    sharpness_score NUMERIC(5,2),
    global_score NUMERIC(5,2),
    final_score NUMERIC(5,2),
    is_blurry BOOLEAN DEFAULT FALSE,
    is_dark BOOLEAN DEFAULT FALSE,
    detected_place TEXT,
    monument_name TEXT,
    detected_scene TEXT,
    scene_label TEXT,
    perceptual_hash TEXT,
    device_model TEXT,
    gps_lat NUMERIC(10,7),
    gps_lon NUMERIC(10,7),
    taken_at TIMESTAMP,
    file_path TEXT,
    original_filename VARCHAR(255)
);

CREATE TABLE detected_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    object_name VARCHAR(255),
    confidence_score NUMERIC(5,2)
);

CREATE TABLE object_photos (
    objects_id UUID REFERENCES detected_objects(id) ON DELETE CASCADE,
    photo_id UUID REFERENCES photos(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (objects_id, photo_id)
);

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    context VARCHAR(50) DEFAULT 'travel' CHECK (context IN ('travel', 'episode', 'photo')),
    source VARCHAR(50) DEFAULT 'user' CHECK (source IN ('user', 'ai', 'system')),
    confidence_score NUMERIC(5,2),
    UNIQUE(label, context)
);

CREATE TABLE travel_tags (
    travel_id UUID REFERENCES travels(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (travel_id, tag_id)
);

CREATE TABLE photo_tags (
    photo_id UUID REFERENCES photos(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (photo_id, tag_id)
);
CREATE TABLE episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    travel_id UUID REFERENCES travels(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,
    title VARCHAR(200),
    location_name VARCHAR(255),
    episode_date DATE,
    intro_text TEXT,
    outro_text TEXT,
    music_mood VARCHAR(200),
    llm_provider VARCHAR(50),
    video_status VARCHAR(20) DEFAULT 'pending',
    video_path TEXT,
    video_filename VARCHAR(255),
    video_duration FLOAT,
    video_size_mb FLOAT,
    video_resolution VARCHAR(20),
    video_codec VARCHAR(20),
    video_fps INT DEFAULT 24,
    render_error TEXT,
    generated_at TIMESTAMP DEFAULT NOW(),
    rendered_at TIMESTAMP
);
CREATE TABLE episode_tags (
    episode_id UUID REFERENCES episodes(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (episode_id, tag_id)
);



CREATE TABLE episode_scenes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID REFERENCES episodes(id) ON DELETE CASCADE,
    scene_number INT NOT NULL,
    photo_id UUID REFERENCES photos(id) ON DELETE SET NULL,
    voiceover_text TEXT,
    screen_text VARCHAR(100),
    duration_seconds FLOAT,
    effect VARCHAR(30)
);

-- Indexes
CREATE INDEX idx_photos_travel ON photos(travel_id);
CREATE INDEX idx_photos_taken_at ON photos(taken_at);
CREATE INDEX idx_photos_scene ON photos(scene_label);
CREATE INDEX idx_photos_hash ON photos(perceptual_hash);
CREATE INDEX idx_photos_duplicate ON photos(duplicate_of);
CREATE INDEX idx_episodes_travel ON episodes(travel_id);
CREATE INDEX idx_episodes_status ON episodes(video_status);
CREATE INDEX idx_episode_scenes_episode ON episode_scenes(episode_id);
CREATE INDEX idx_travels_user ON travels(user_id);
CREATE INDEX idx_photos_place ON photos(detected_place);
CREATE INDEX idx_photos_detected_scene ON photos(detected_scene);
CREATE INDEX idx_photo_tags_photo ON photo_tags(photo_id);
CREATE INDEX idx_travel_tags_travel ON travel_tags(travel_id);
CREATE INDEX idx_travel_tags_tag ON travel_tags(tag_id);
CREATE INDEX idx_episode_tags_episode ON episode_tags(episode_id);
CREATE INDEX idx_episode_tags_tag ON episode_tags(tag_id);
CREATE INDEX idx_tags_label ON tags(label);

-- Fonctions
CREATE OR REPLACE FUNCTION update_photo_metadata(
    p_photo_id UUID,
    p_nettete NUMERIC,
    p_luminosite NUMERIC,
    p_hash TEXT,
    p_scene TEXT,
    p_latitude NUMERIC,
    p_longitude NUMERIC,
    p_appareil TEXT,
    p_date TIMESTAMP,
    p_score NUMERIC
)
RETURNS VOID AS $$
BEGIN
    UPDATE photos SET 
        sharpness_score = p_nettete,
        brightness_score = p_luminosite,
        perceptual_hash = p_hash,
        scene_label = p_scene,
        gps_lat = p_latitude,
        gps_lon = p_longitude,
        device_model = p_appareil,
        taken_at = p_date,
        final_score = p_score
    WHERE id = p_photo_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_travel(
    p_user_id UUID,
    p_title VARCHAR,
    p_destination VARCHAR DEFAULT NULL,
    p_description TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_travel_id UUID;
BEGIN
    INSERT INTO travels (user_id, title, destination, description, created_at) 
    VALUES (p_user_id, p_title, p_destination, p_description, NOW()) 
    RETURNING id INTO v_travel_id;
    
    RETURN v_travel_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_tag_to_travel(
    p_travel_id UUID,
    p_tag_label VARCHAR,
    p_source VARCHAR DEFAULT 'user',
    p_confidence NUMERIC DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_tag_id UUID;
BEGIN
    INSERT INTO tags (label, created_at, context, source, confidence_score) 
    VALUES (p_tag_label, NOW(), 'travel', p_source, p_confidence)
    ON CONFLICT (label, context) DO UPDATE 
    SET source = EXCLUDED.source,
        confidence_score = COALESCE(EXCLUDED.confidence_score, tags.confidence_score)
    RETURNING id INTO v_tag_id;
    
    INSERT INTO travel_tags (travel_id, tag_id) 
    VALUES (p_travel_id, v_tag_id)
    ON CONFLICT DO NOTHING;
    
    RETURN v_tag_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_tag_to_episode(
    p_episode_id UUID,
    p_tag_label VARCHAR,
    p_source VARCHAR DEFAULT 'user',
    p_confidence NUMERIC DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_tag_id UUID;
BEGIN
    INSERT INTO tags (label, created_at, context, source, confidence_score) 
    VALUES (p_tag_label, NOW(), 'episode', p_source, p_confidence)
    ON CONFLICT (label, context) DO UPDATE 
    SET source = EXCLUDED.source,
        confidence_score = COALESCE(EXCLUDED.confidence_score, tags.confidence_score)
    RETURNING id INTO v_tag_id;
    
    INSERT INTO episode_tags (episode_id, tag_id) 
    VALUES (p_episode_id, v_tag_id)
    ON CONFLICT DO NOTHING;
    
    RETURN v_tag_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_travels_by_tag(p_tag_label VARCHAR)
RETURNS TABLE(
    id UUID,
    title VARCHAR,
    destination VARCHAR,
    description TEXT,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT t.id, t.title, t.destination, t.description, t.created_at
    FROM travels t
    JOIN travel_tags tt ON tt.travel_id = t.id
    JOIN tags tg ON tg.id = tt.tag_id
    WHERE tg.label = p_tag_label AND tg.context = 'travel'
    ORDER BY t.created_at DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_travel_tags(p_travel_id UUID)
RETURNS TABLE(
    tag_id UUID,
    label VARCHAR,
    created_at TIMESTAMP,
    source VARCHAR,
    confidence_score NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT tg.id, tg.label, tg.created_at, tg.source, tg.confidence_score
    FROM tags tg
    JOIN travel_tags tt ON tt.tag_id = tg.id
    WHERE tt.travel_id = p_travel_id
    ORDER BY tg.label ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_episode_tags(p_episode_id UUID)
RETURNS TABLE(
    tag_id UUID,
    label VARCHAR,
    created_at TIMESTAMP,
    source VARCHAR,
    confidence_score NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT tg.id, tg.label, tg.created_at, tg.source, tg.confidence_score
    FROM tags tg
    JOIN episode_tags et ON et.tag_id = tg.id
    WHERE et.episode_id = p_episode_id
    ORDER BY tg.label ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_episodes_by_tag(p_tag_label VARCHAR, p_travel_id UUID DEFAULT NULL)
RETURNS TABLE(
    id UUID,
    episode_number INT,
    title VARCHAR,
    location_name VARCHAR,
    episode_date DATE,
    travel_id UUID,
    travel_title VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT e.id, e.episode_number, e.title, e.location_name, e.episode_date,
           e.travel_id, t.title AS travel_title
    FROM episodes e
    JOIN episode_tags et ON et.episode_id = e.id
    JOIN tags tg ON tg.id = et.tag_id
    LEFT JOIN travels t ON t.id = e.travel_id
    WHERE tg.label = p_tag_label 
      AND tg.context = 'episode'
      AND (p_travel_id IS NULL OR e.travel_id = p_travel_id)
    ORDER BY e.episode_number ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_tags_by_context(p_context VARCHAR)
RETURNS TABLE(
    id UUID,
    label VARCHAR,
    created_at TIMESTAMP,
    source VARCHAR,
    confidence_score NUMERIC,
    usage_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT tg.id, tg.label, tg.created_at, tg.source, tg.confidence_score,
           COUNT(CASE 
               WHEN tg.context = 'travel' THEN tt.travel_id
               WHEN tg.context = 'episode' THEN et.episode_id
               WHEN tg.context = 'photo' THEN pt.photo_id
           END) AS usage_count
    FROM tags tg
    LEFT JOIN travel_tags tt ON tt.tag_id = tg.id
    LEFT JOIN episode_tags et ON et.tag_id = tg.id
    LEFT JOIN photo_tags pt ON pt.tag_id = tg.id
    WHERE tg.context = p_context
    GROUP BY tg.id
    ORDER BY usage_count DESC, tg.label ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION search_travels_and_episodes_by_tag(p_search TEXT)
RETURNS TABLE(
    type VARCHAR(20),
    id UUID,
    title VARCHAR,
    destination VARCHAR,
    description TEXT,
    tags TEXT[],
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 'travel'::VARCHAR(20) AS type, 
           t.id, t.title, t.destination, t.description,
           ARRAY_AGG(DISTINCT tg.label) AS tags,
           t.created_at
    FROM travels t
    LEFT JOIN travel_tags tt ON tt.travel_id = t.id
    LEFT JOIN tags tg ON tg.id = tt.tag_id
    WHERE tg.label ILIKE '%' || p_search || '%'
       OR t.title ILIKE '%' || p_search || '%'
       OR t.destination ILIKE '%' || p_search || '%'
    GROUP BY t.id
    
    UNION ALL
    
    SELECT 'episode'::VARCHAR(20) AS type,
           e.id, e.title, e.location_name, e.intro_text,
           ARRAY_AGG(DISTINCT tg.label) AS tags,
           e.generated_at
    FROM episodes e
    LEFT JOIN episode_tags et ON et.episode_id = e.id
    LEFT JOIN tags tg ON tg.id = et.tag_id
    WHERE tg.label ILIKE '%' || p_search || '%'
       OR e.title ILIKE '%' || p_search || '%'
       OR e.location_name ILIKE '%' || p_search || '%'
       OR e.intro_text ILIKE '%' || p_search || '%'
    GROUP BY e.id
    
    ORDER BY created_at DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_travel_photos(p_travel_id UUID)
RETURNS TABLE(file_path TEXT) AS $$
BEGIN
    RETURN QUERY
    SELECT photos.file_path 
    FROM photos 
    WHERE photos.travel_id = p_travel_id 
    ORDER BY photos.taken_at ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION auto_tag_episode(
    p_episode_id UUID,
    p_ai_generated_tags TEXT[],
    p_confidence_threshold NUMERIC DEFAULT 0.7
)
RETURNS VOID AS $$
DECLARE
    v_tag_label TEXT;
    v_tag_id UUID;
BEGIN
    FOREACH v_tag_label IN ARRAY p_ai_generated_tags
    LOOP
        INSERT INTO tags (label, created_at, context, source, confidence_score) 
        VALUES (v_tag_label, NOW(), 'episode', 'ai', p_confidence_threshold)
        ON CONFLICT (label, context) DO UPDATE 
        SET source = EXCLUDED.source,
            confidence_score = COALESCE(EXCLUDED.confidence_score, tags.confidence_score)
        RETURNING id INTO v_tag_id;
        
        INSERT INTO episode_tags (episode_id, tag_id) 
        VALUES (p_episode_id, v_tag_id)
        ON CONFLICT DO NOTHING;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_travel_all_tags(p_travel_id UUID)
RETURNS TABLE(
    tag_id UUID,
    label VARCHAR,
    context VARCHAR,
    source VARCHAR,
    confidence_score NUMERIC,
    linked_to_episode BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT tg.id, tg.label, tg.context, tg.source, tg.confidence_score, FALSE AS linked_to_episode
    FROM tags tg
    JOIN travel_tags tt ON tt.tag_id = tg.id
    WHERE tt.travel_id = p_travel_id
    
    UNION
    
    SELECT DISTINCT tg.id, tg.label, tg.context, tg.source, tg.confidence_score, TRUE AS linked_to_episode
    FROM tags tg
    JOIN episode_tags et ON et.tag_id = tg.id
    JOIN episodes e ON e.id = et.episode_id
    WHERE e.travel_id = p_travel_id
    
    ORDER BY label ASC;
END;
$$ LANGUAGE plpgsql;
