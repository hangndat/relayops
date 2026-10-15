-- RelayOps Day 04 foundation schema

create table api_clients (
  id uuid primary key,
  name text not null,
  api_key_hash text not null unique,
  created_at timestamptz not null default now()
);

create table destinations (
  id uuid primary key,
  api_client_id uuid not null references api_clients (id) on delete cascade,
  name text not null,
  target_url text not null,
  signing_secret text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  constraint destinations_name_not_blank check (char_length(trim(name)) > 0),
  constraint destinations_url_https_or_http check (target_url ~* '^https?://')
);

create index destinations_api_client_id_idx on destinations (api_client_id);

create table inbound_events (
  id uuid primary key,
  api_client_id uuid not null references api_clients (id) on delete cascade,
  destination_id uuid not null references destinations (id) on delete cascade,
  idempotency_key text not null,
  payload jsonb not null,
  status text not null,
  attempt_count int not null default 0,
  next_attempt_at timestamptz,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint inbound_events_status_check check (
    status in (
      'RECEIVED',
      'QUEUED',
      'DELIVERING',
      'SUCCEEDED',
      'FAILED_RETRYABLE',
      'FAILED_TERMINAL'
    )
  ),
  constraint inbound_events_idempotency_unique unique (api_client_id, idempotency_key)
);

create index inbound_events_status_next_attempt_idx
  on inbound_events (status, next_attempt_at);

create table delivery_attempts (
  id uuid primary key,
  event_id uuid not null references inbound_events (id) on delete cascade,
  attempt_number int not null,
  started_at timestamptz not null,
  finished_at timestamptz,
  http_status int,
  success boolean not null default false,
  error_message text,
  response_body_excerpt text,
  constraint delivery_attempts_event_attempt_unique unique (event_id, attempt_number)
);

create index delivery_attempts_event_id_idx on delivery_attempts (event_id);
