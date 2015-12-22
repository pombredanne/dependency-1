drop table if exists tokens;

create table tokens (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  tag                     text not null check(lower_non_empty_trimmed_string(tag)),
  token                   text not null check (trim(token) = token)
);

select schema_evolution_manager.create_basic_audit_data('public', 'tokens');

comment on table tokens is '
  Stores oauth tokens for a given user.
';

comment on column tokens.tag is '
  Identifies the token - e.g. github_oauth
';

create index on tokens(user_guid);
create unique index tokens_user_guid_tag_not_deleted_un_idx on tokens(user_guid, tag) where deleted_at is null;
