alter table if exists public.contribution_links
    add column if not exists opened_at timestamp without time zone;

alter table if exists public.contribution_links
    add column if not exists upload_count integer not null default 0;

alter table if exists public.contribution_links
    add column if not exists last_upload_at timestamp without time zone;

create index if not exists contribution_links_owner_created_idx
    on public.contribution_links (owner_id, created_at desc);
