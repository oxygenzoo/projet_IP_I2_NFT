alter table if exists public.users
    add column if not exists language varchar(8) not null default 'fr';

alter table if exists public.profiles
    add column if not exists language varchar(8) not null default 'fr';

alter table if exists public.episodes
    add column if not exists share_token varchar(96);

create unique index if not exists episodes_share_token_idx
    on public.episodes (share_token)
    where share_token is not null;

create table if not exists public.episode_collaborators (
    id uuid primary key default gen_random_uuid(),
    episode_id uuid not null references public.episodes(id) on delete cascade,
    user_id uuid,
    email varchar(255),
    role varchar(32) not null default 'viewer',
    created_at timestamp without time zone not null default now(),
    constraint episode_collaborators_identity_check check (user_id is not null or email is not null),
    constraint episode_collaborators_role_check check (role in ('viewer', 'contributor'))
);

create index if not exists episode_collaborators_episode_idx
    on public.episode_collaborators (episode_id);

create index if not exists episode_collaborators_user_idx
    on public.episode_collaborators (user_id);

create index if not exists episode_collaborators_email_idx
    on public.episode_collaborators (lower(email));

create table if not exists public.contribution_links (
    id uuid primary key default gen_random_uuid(),
    travel_id uuid not null references public.travels(id) on delete cascade,
    token varchar(96) not null unique,
    owner_id uuid not null,
    expires_at timestamp without time zone,
    created_at timestamp without time zone not null default now()
);

create index if not exists contribution_links_travel_idx
    on public.contribution_links (travel_id);

create table if not exists public.creation_sessions (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null,
    travel_id uuid,
    episode_id uuid,
    status varchar(32) not null,
    result_video_url varchar(2048),
    error_message text,
    created_at timestamp without time zone not null default now(),
    updated_at timestamp without time zone not null default now(),
    constraint creation_sessions_status_check check (status in ('uploading', 'preferences', 'generating', 'done', 'error'))
);

create index if not exists creation_sessions_owner_status_idx
    on public.creation_sessions (owner_id, status, updated_at desc);
