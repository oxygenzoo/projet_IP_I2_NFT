insert into public.profiles (id, email, created_at, updated_at, language)
select
    u.id,
    u.email,
    now(),
    now(),
    coalesce(nullif(u.language, ''), 'fr')
from public.users u
where not exists (
    select 1
    from public.profiles p
    where p.id = u.id
);

insert into public.users (id, email, password_hash, consent_rgpd, consent_date, language)
select
    p.id,
    p.id::text || '@external.local',
    'external',
    true,
    now(),
    coalesce(nullif(p.language, ''), 'fr')
from public.profiles p
where not exists (
    select 1
    from public.users u
    where u.id = p.id
);
