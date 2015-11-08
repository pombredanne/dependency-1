drop table if exists authorizations;
drop table if exists users;

create table users (
  guid                    uuid primary key,
  email                   text not null check(non_empty_trimmed_string(email))
);

comment on table users is '
  Central user database
';

select schema_evolution_manager.create_basic_audit_data('public', 'users');
create unique index users_lower_email_not_deleted_un_idx on users(lower(email)) where deleted_at is null;


create table authorizations (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  scms                    text not null check(enum(scms)),
  token                   text not null check(non_empty_trimmed_string(token))
);

comment on table authorizations is '
  Stores authorization tokens granted to a user to a specific SCMS
  (e.g. the oauth token to access github).
';

select schema_evolution_manager.create_basic_audit_data('public', 'authorizations');
create index on authorizations(user_guid);
create unique index authorizations_user_guid_scms_token_not_deleted_un_idx
    on authorizations(user_guid, scms, token)
    where deleted_at is null;

