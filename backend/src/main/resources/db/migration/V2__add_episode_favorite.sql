alter table episodes
    add column if not exists favorite boolean not null default false;
