drop table if exists github_users;

create table github_users (
  guid                     uuid not null primary key,
  user_guid                uuid not null references users,
  id                       bigint not null,
  login                    text not null check(util.non_empty_trimmed_string(login))
);

select audit.setup('public', 'github_users');
create index on github_users(user_guid);
create unique index github_users_id_not_deleted_un_idx on github_users(id) where deleted_at is null;
create unique index github_users_login_not_deleted_un_idx on github_users(login) where deleted_at is null;

comment on table github_users is '
  maps our users to their IDs in third party systems (e.g. github)
';

