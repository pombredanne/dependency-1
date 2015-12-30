drop table if exists github_users;

create table github_users (
  id                       text not null primary key,
  user_id                  text not null references users,
  github_user_id           bigint not null,
  login                    text not null check(util.non_empty_trimmed_string(login))
);

select audit.setup('public', 'github_users');
create index on github_users(user_id);
create unique index github_users_id_not_deleted_un_idx on github_users(github_user_id) where deleted_at is null;
create unique index github_users_login_not_deleted_un_idx on github_users(login) where deleted_at is null;

comment on table github_users is '
  Maps our users to their IDs in third party systems (e.g. github)
';

