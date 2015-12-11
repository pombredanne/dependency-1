drop table if exists last_emails;

create table last_emails (
  guid                       uuid primary key,
  user_guid                  uuid not null references users,
  publication                text not null check(enum(publication))
);

comment on table last_emails is '
  For publications like the daily email, records when we last
  generated an email to a user.
';

select schema_evolution_manager.create_basic_audit_data('public', 'last_emails');
create index on last_emails(user_guid);

create unique index last_emails_user_guid_publication_not_deleted_un_idx
    on last_emails(user_guid, publication)
 where deleted_at is null;
