# Documentation de la Base de Données PostgreSQL

## Structure de la Base de Données

Cette documentation décrit la structure complète de la base de données PostgreSQL pour l'application de gestion de voyages, photos et épisodes vidéo.

### Configuration Initiale

```sql
ALTER TABLESPACE pg_default OWNER TO postgres;
```

---

## Tables Principales

### Table `users`

Gère les utilisateurs de l'application.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant unique de l'utilisateur |
| `email` | VARCHAR(255) | UNIQUE NOT NULL | Adresse email de l'utilisateur |
| `password_hash` | TEXT | NOT NULL | Hash du mot de passe |
| `last_login` | TIMESTAMP | | Date du dernier accès |
| `consent_rgpd` | BOOLEAN | DEFAULT FALSE | Consentement RGPD |
| `consent_date` | TIMESTAMP | | Date du consentement |

---

### Table `travels`

Stocke les voyages créés par les utilisateurs.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant unique du voyage |
| `user_id` | UUID | REFERENCES users(id) ON DELETE CASCADE | Référence à l'utilisateur propriétaire |
| `title` | VARCHAR(200) | NOT NULL | Titre du voyage |
| `destination` | VARCHAR(200) | | Destination du voyage |
| `description` | TEXT | | Description détaillée |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Date de création |

---

### Table `photos`

Contient les photos importées et leurs métadonnées enrichies.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant unique de la photo |
| `user_id` | UUID | REFERENCES users(id) ON DELETE CASCADE | Propriétaire de la photo |
| `travel_id` | UUID | REFERENCES travels(id) ON DELETE CASCADE | Voyage associé |
| `filename` | TEXT | NOT NULL | Nom du fichier |
| `storage_url` | TEXT | NOT NULL | URL de stockage |
| `format` | VARCHAR(20) | | Format de l'image |
| `capture_date` | TIMESTAMP | | Date de capture |
| `latitude` | NUMERIC(10,7) | | Coordonnée GPS latitude |
| `longitude` | NUMERIC(10,7) | | Coordonnée GPS longitude |
| `width` | INT | | Largeur en pixels |
| `height` | INT | | Hauteur en pixels |
| `size_mb` | NUMERIC(8,2) | | Taille en Mo |
| `imported_at` | TIMESTAMP | DEFAULT NOW() | Date d'importation |
| `is_duplicate` | BOOLEAN | DEFAULT FALSE | Indique si c'est un doublon |
| `duplicate_of` | UUID | REFERENCES photos(id) ON DELETE SET NULL | Référence à la photo originale |
| `brightness_score` | NUMERIC(5,2) | | Score de luminosité (0-100) |
| `sharpness_score` | NUMERIC(5,2) | | Score de netteté (0-100) |
| `global_score` | NUMERIC(5,2) | | Score global de qualité |
| `final_score` | NUMERIC(5,2) | | Score final après analyse |
| `is_blurry` | BOOLEAN | DEFAULT FALSE | Photo floue |
| `is_dark` | BOOLEAN | DEFAULT FALSE | Photo trop sombre |
| `detected_place` | TEXT | | Lieu détecté automatiquement |
| `monument_name` | TEXT | | Nom du monument détecté |
| `detected_scene` | TEXT | | Scène détectée |
| `scene_label` | TEXT | | Libellé de la scène |
| `perceptual_hash` | TEXT | | Hash perceptuel pour détection doublons |
| `device_model` | TEXT | | Modèle de l'appareil photo |
| `gps_lat` | NUMERIC(10,7) | | Latitude GPS (format normalisé) |
| `gps_lon` | NUMERIC(10,7) | | Longitude GPS (format normalisé) |
| `taken_at` | TIMESTAMP | | Date et heure de prise de vue |
| `file_path` | TEXT | | Chemin du fichier local |
| `original_filename` | VARCHAR(255) | | Nom de fichier original |

---

### Table `detected_objects`

Stocke les objets détectés dans les photos.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant de l'objet |
| `object_name` | VARCHAR(255) | | Nom de l'objet détecté |
| `confidence_score` | NUMERIC(5,2) | | Niveau de confiance de la détection |

---

### Table `object_photos`

Association entre les objets détectés et les photos.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `objects_id` | UUID | REFERENCES detected_objects(id) ON DELETE CASCADE | Référence à l'objet |
| `photo_id` | UUID | REFERENCES photos(id) ON DELETE CASCADE | Référence à la photo |
| `added_at` | TIMESTAMP | DEFAULT NOW() | Date d'ajout |
| **Clé primaire** | | PRIMARY KEY (objects_id, photo_id) | Association unique |

---

### Table `tags`

Gestion des tags avec contexte et source.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant du tag |
| `label` | VARCHAR(255) | NOT NULL | Libellé du tag |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Date de création |
| `context` | VARCHAR(50) | DEFAULT 'travel', CHECK IN ('travel', 'episode', 'photo') | Contexte d'utilisation |
| `source` | VARCHAR(50) | DEFAULT 'user', CHECK IN ('user', 'ai', 'system') | Source de création |
| `confidence_score` | NUMERIC(5,2) | | Score de confiance (pour tags IA) |
| **Contrainte** | | UNIQUE(label, context) | Unicité par contexte |

---

### Table `travel_tags`

Association entre les voyages et leurs tags.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `travel_id` | UUID | REFERENCES travels(id) ON DELETE CASCADE | Référence au voyage |
| `tag_id` | UUID | REFERENCES tags(id) ON DELETE CASCADE | Référence au tag |
| **Clé primaire** | | PRIMARY KEY (travel_id, tag_id) | Association unique |

---

### Table `photo_tags`

Association entre les photos et leurs tags.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `photo_id` | UUID | REFERENCES photos(id) ON DELETE CASCADE | Référence à la photo |
| `tag_id` | UUID | REFERENCES tags(id) ON DELETE CASCADE | Référence au tag |
| **Clé primaire** | | PRIMARY KEY (photo_id, tag_id) | Association unique |

---

### Table `episode_tags`

Association entre les épisodes et leurs tags.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `episode_id` | UUID | REFERENCES episodes(id) ON DELETE CASCADE | Référence à l'épisode |
| `tag_id` | UUID | REFERENCES tags(id) ON DELETE CASCADE | Référence au tag |
| **Clé primaire** | | PRIMARY KEY (episode_id, tag_id) | Association unique |

---

### Table `episodes`

Gestion des épisodes vidéo générés.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant de l'épisode |
| `travel_id` | UUID | REFERENCES travels(id) ON DELETE CASCADE | Voyage associé |
| `episode_number` | INT | NOT NULL | Numéro de l'épisode |
| `title` | VARCHAR(200) | | Titre de l'épisode |
| `location_name` | VARCHAR(255) | | Nom du lieu |
| `episode_date` | DATE | | Date de l'épisode |
| `intro_text` | TEXT | | Texte d'introduction |
| `outro_text` | TEXT | | Texte de conclusion |
| `music_mood` | VARCHAR(200) | | Ambiance musicale |
| `llm_provider` | VARCHAR(50) | | Fournisseur LLM utilisé |
| `video_status` | VARCHAR(20) | DEFAULT 'pending' | Statut de la vidéo |
| `video_path` | TEXT | | Chemin de la vidéo |
| `video_filename` | VARCHAR(255) | | Nom du fichier vidéo |
| `video_duration` | FLOAT | | Durée en secondes |
| `video_size_mb` | FLOAT | | Taille en Mo |
| `video_resolution` | VARCHAR(20) | | Résolution vidéo |
| `video_codec` | VARCHAR(20) | | Codec utilisé |
| `video_fps` | INT | DEFAULT 24 | Images par seconde |
| `render_error` | TEXT | | Message d'erreur de rendu |
| `generated_at` | TIMESTAMP | DEFAULT NOW() | Date de génération |
| `rendered_at` | TIMESTAMP | | Date de rendu final |

---

### Table `episode_scenes`

Scènes composant chaque épisode.

| Colonne | Type | Contrainte | Description |
|---------|------|------------|-------------|
| `id` | UUID | PRIMARY KEY DEFAULT gen_random_uuid() | Identifiant de la scène |
| `episode_id` | UUID | REFERENCES episodes(id) ON DELETE CASCADE | Épisode associé |
| `scene_number` | INT | NOT NULL | Numéro de la scène |
| `photo_id` | UUID | REFERENCES photos(id) ON DELETE SET NULL | Photo associée |
| `voiceover_text` | TEXT | | Texte de voix-off |
| `screen_text` | VARCHAR(100) | | Texte à l'écran |
| `duration_seconds` | FLOAT | | Durée en secondes |
| `effect` | VARCHAR(30) | | Effet visuel |

---

## Indexes

### Index sur la table `photos`
```sql
CREATE INDEX idx_photos_travel ON photos(travel_id);
CREATE INDEX idx_photos_taken_at ON photos(taken_at);
CREATE INDEX idx_photos_scene ON photos(scene_label);
CREATE INDEX idx_photos_hash ON photos(perceptual_hash);
CREATE INDEX idx_photos_duplicate ON photos(duplicate_of);
CREATE INDEX idx_photos_place ON photos(detected_place);
CREATE INDEX idx_photos_detected_scene ON photos(detected_scene);
```

### Index sur la table `episodes`
```sql
CREATE INDEX idx_episodes_travel ON episodes(travel_id);
CREATE INDEX idx_episodes_status ON episodes(video_status);
```

### Index sur la table `episode_scenes`
```sql
CREATE INDEX idx_episode_scenes_episode ON episode_scenes(episode_id);
```

### Index sur la table `travels`
```sql
CREATE INDEX idx_travels_user ON travels(user_id);
```

### Index sur les tables d'association
```sql
CREATE INDEX idx_photo_tags_photo ON photo_tags(photo_id);
CREATE INDEX idx_travel_tags_travel ON travel_tags(travel_id);
CREATE INDEX idx_travel_tags_tag ON travel_tags(tag_id);
CREATE INDEX idx_episode_tags_episode ON episode_tags(episode_id);
CREATE INDEX idx_episode_tags_tag ON episode_tags(tag_id);
```

### Index sur la table `tags`
```sql
CREATE INDEX idx_tags_label ON tags(label);
```

---

## Fonctions Stockées

### Gestion des Photos

| Fonction | Description |
|----------|-------------|
| `update_photo_metadata()` | Met à jour les métadonnées d'une photo (netteté, luminosité, hash, scène, GPS, appareil, date, score) |

### Gestion des Voyages

| Fonction | Description |
|----------|-------------|
| `create_travel()` | Crée un nouveau voyage pour un utilisateur |
| `get_travel_photos()` | Récupère les chemins des photos d'un voyage |

### Gestion des Tags

| Fonction | Description |
|----------|-------------|
| `add_tag_to_travel()` | Ajoute un tag à un voyage |
| `add_tag_to_episode()` | Ajoute un tag à un épisode |
| `auto_tag_episode()` | Tagge automatiquement un épisode avec des tags générés par IA |

### Consultation des Tags

| Fonction | Description |
|----------|-------------|
| `get_travel_tags()` | Récupère tous les tags d'un voyage |
| `get_episode_tags()` | Récupère tous les tags d'un épisode |
| `get_travels_by_tag()` | Récupère les voyages associés à un tag |
| `get_episodes_by_tag()` | Récupère les épisodes associés à un tag |
| `get_tags_by_context()` | Récupère tous les tags d'un contexte avec compteur d'utilisation |
| `get_travel_all_tags()` | Récupère tous les tags d'un voyage (incluant ceux de ses épisodes) |

### Recherche

| Fonction | Description |
|----------|-------------|
| `search_travels_and_episodes_by_tag()` | Recherche des voyages et épisodes par tag ou texte |

---

## Diagramme des Relations

```
users (1) ──── (n) travels (1) ──── (n) episodes (1) ──── (n) episode_scenes
  │                   │                    │
  │                   │                    ├── (n) episode_tags (n) ── tags
  │                   │                    │
  │                   └── (n) photos       │
  │                        │               │
  │                        ├── (n) photo_tags (n)
  │                        │
  │                        ├── (n) object_photos (n) ── detected_objects
  │                        │
  │                        └── (1) photos (duplicate_of)
  │
  └── (n) travels ──── (n) travel_tags (n) ── tags
```

---

## Exemples d'Utilisation

### Création d'un Voyage avec Tags

```sql
-- 1. Créer le voyage
SELECT create_travel(
    'user-uuid',
    'Road Trip en Bretagne',
    'Bretagne, France',
    'Un merveilleux road trip le long des côtes bretonnes'
);

-- 2. Ajouter des tags au voyage
SELECT add_tag_to_travel('travel-uuid', 'Bretagne');
SELECT add_tag_to_travel('travel-uuid', 'Road Trip');

-- 3. Ajouter une photo
INSERT INTO photos (id, user_id, travel_id, filename, storage_url, file_path)
VALUES (
    'photo-uuid',
    'user-uuid',
    'travel-uuid',
    'plage.jpg',
    'https://storage.example.com/plage.jpg',
    '/photos/plage.jpg'
);

-- 4. Mettre à jour les métadonnées
SELECT update_photo_metadata(
    'photo-uuid', 85.5, 72.3, 'hash456', 'plage',
    48.8566, 2.3522, 'Sony Alpha', '2024-01-15 14:30:00', 78.9
);
```

### Gestion des Épisodes

```sql
-- 1. Créer un épisode
INSERT INTO episodes (
    travel_id, episode_number, title, location_name, episode_date
) VALUES (
    'travel-uuid', 1, 'Jour 1 - Arrivée à Saint-Malo',
    'Saint-Malo, France', '2024-01-15'
);

-- 2. Ajouter des tags
SELECT add_tag_to_episode('episode-uuid', 'Saint-Malo');

-- 3. Tag automatique par IA
SELECT auto_tag_episode(
    'episode-uuid',
    ARRAY['patrimoine', 'architecture', 'histoire'],
    0.85
);

-- 4. Ajouter une scène
INSERT INTO episode_scenes (
    episode_id, scene_number, photo_id, voiceover_text, duration_seconds
) VALUES (
    'episode-uuid', 1, 'photo-uuid',
    'Découverte des remparts de Saint-Malo', 5.0
);
```

### Recherche et Consultation

```sql
-- Récupérer tous les tags d'un voyage
SELECT * FROM get_travel_all_tags('travel-uuid');

-- Rechercher des contenus par tag
SELECT * FROM search_travels_and_episodes_by_tag('Bretagne');

-- Lister les épisodes par tag
SELECT * FROM get_episodes_by_tag('patrimoine');

-- Statistiques des tags par contexte
SELECT * FROM get_tags_by_context('episode');
```

---

## Notes sur la Performance

1. **Indexation** : Les index sont optimisés pour les requêtes les plus courantes (recherche par voyage, par date, par tag).

2. **Hash Perceptuel** : Le champ `perceptual_hash` permet une détection efficace des doublons.

3. **Scores** : Les champs de score utilisent le type `NUMERIC(5,2)` pour un bon équilibre entre précision et performance.

4. **Contraintes Check** : Les contraintes sur le contexte et la source des tags garantissent l'intégrité des données.

5. **Cascades** : Les suppressions en cascade assurent une gestion propre des dépendances.

---

## Maintenance Recommandée

### Vérification de l'Intégrité

```sql
-- Vérifier les photos orphelines
SELECT p.* FROM photos p 
LEFT JOIN travels t ON t.id = p.travel_id 
WHERE t.id IS NULL;

-- Vérifier les tags sans association
SELECT tg.* FROM tags tg
LEFT JOIN travel_tags tt ON tt.tag_id = tg.id
LEFT JOIN episode_tags et ON et.tag_id = tg.id
LEFT JOIN photo_tags pt ON pt.tag_id = tg.id
WHERE tt.tag_id IS NULL 
  AND et.tag_id IS NULL 
  AND pt.tag_id IS NULL;
```

### Nettoyage

```sql
-- Supprimer les voyages vides (sans photos)
DELETE FROM travels 
WHERE id NOT IN (SELECT DISTINCT travel_id FROM photos);

-- Supprimer les épisodes sans scènes
DELETE FROM episodes 
WHERE id NOT IN (SELECT DISTINCT episode_id FROM episode_scenes);
```

---

**Version :** 1.0.0