drop table if exists tokens;

create table tokens (
  id                      text primary key,
  user_id                 text not null references users,
  tag                     text not null check(util.lower_non_empty_trimmed_string(tag)),
  token                   text not null check (trim(token) = token)
);

select audit.setup('public', 'tokens');

comment on table tokens is '
  Stores oauth tokens for a given user.
';

comment on column tokens.tag is '
  Identifies the token - e.g. github_oauth
';

create index on tokens(user_id);
create unique index tokens_user_id_tag_not_deleted_un_idx on tokens(user_id, tag) where deleted_at is null;
