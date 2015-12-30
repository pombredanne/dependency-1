drop table if exists subscriptions;

create table subscriptions (
  id                       text primary key,
  user_id                  text not null references users,
  publication                text not null check(util.lower_non_empty_trimmed_string(publication))
);

comment on table subscriptions is '
  Keeps track of things the user has subscribed to (like a daily email)
';

select audit.setup('public', 'subscriptions');
create index on subscriptions(user_id);

create unique index subscriptions_user_id_publication_not_deleted_un_idx
    on subscriptions(user_id, publication)
 where deleted_at is null;
