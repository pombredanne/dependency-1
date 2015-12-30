drop table if exists last_emails;

create table last_emails (
  id                       text primary key,
  user_id                  text not null references users,
  publication                text not null check(util.lower_non_empty_trimmed_string(publication))
);

comment on table last_emails is '
  For publications like the daily email, records when we last
  generated an email to a user.
';

select audit.setup('public', 'last_emails');
create index on last_emails(user_id);

create unique index last_emails_user_id_publication_not_deleted_un_idx
    on last_emails(user_id, publication)
 where deleted_at is null;
