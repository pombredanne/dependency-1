drop table if exists user_organizations;

create table user_organizations (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  organization_guid       uuid not null references organizations
);

comment on table user_organizations is '
  Each user is assigned a single organization to represent their own
  projects. This table records the org assigned to a user.
';

select schema_evolution_manager.create_basic_audit_data('public', 'user_organizations');
create index on user_organizations(user_guid);
create index on user_organizations(organization_guid);
create unique index user_organizations_user_guid_not_del_idx on user_organizations(user_guid) where deleted_at is null;
create unique index user_organizations_organization_guid_not_del_idx on user_organizations(organization_guid) where deleted_at is null;

