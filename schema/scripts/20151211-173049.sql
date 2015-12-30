drop table if exists subscriptions;

create table subscriptions (
  guid                       uuid primary key,
  user_guid                  uuid not null references users,
  publication                text not null check(util.lower_non_empty_trimmed_string(publication))
);

comment on table subscriptions is '
  Keeps track of things the user has subscribed to (like a daily email)
';

select audit.setup('public', 'subscriptions');
create index on subscriptions(user_guid);

create unique index subscriptions_user_guid_publication_not_deleted_un_idx
    on subscriptions(user_guid, publication)
 where deleted_at is null;
