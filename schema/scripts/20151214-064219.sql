drop table if exists memberships;

create table memberships (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  organization_guid       uuid not null references organizations,
  role                    text not null check(lower_non_empty_trimmed_string(role))
);

comment on table memberships is '
  Users can join other organizations. Note that the user_organizations table
  records the specific organization assigned to a user while this table lists
  all the members of an org and is used to represent group accounts (e.g. an
  organization representing a company). Note that we only allow one row
  per user/org - and we store only the higher role (e.g. admin).
';

select schema_evolution_manager.create_basic_audit_data('public', 'memberships');
create index on memberships(user_guid);
create index on memberships(organization_guid);
create unique index memberships_user_guid_organization_guid_not_del_idx on memberships(user_guid, organization_guid) where deleted_at is null;
