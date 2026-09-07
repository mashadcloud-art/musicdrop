-- FileDrop share history — run this in the Supabase SQL editor.
-- Stores only metadata about a completed upload (never the file itself,
-- and never a cloud OAuth token). Row Level Security keys every row to the
-- anonymous Supabase user created on-device, so one person's share history
-- is never visible to anyone else, including other FileDrop installs.

create extension if not exists "pgcrypto";

create table if not exists public.shares (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  provider text not null check (provider in ('google', 'dropbox')),
  file_name text not null,
  file_id text not null,
  link text not null,
  created_at timestamptz not null default now()
);

create index if not exists shares_owner_created_idx
  on public.shares (owner_id, created_at desc);

alter table public.shares enable row level security;

-- Anonymous Supabase auth (see src/lib/supabase.ts) still yields a real
-- auth.uid(), so these policies apply to every device the same way.
create policy "read own shares"
  on public.shares for select
  using (auth.uid() = owner_id);

create policy "insert own shares"
  on public.shares for insert
  with check (auth.uid() = owner_id);

-- No update/delete policy on purpose — history is append-only from the app.

-- In Supabase Dashboard → Authentication → Sign In / Providers, make sure
-- "Allow anonymous sign-ins" is turned on, or signInAnonymously() will fail
-- and share history will just silently stay empty (upload/link creation is
-- unaffected either way, since that never touches Supabase).
