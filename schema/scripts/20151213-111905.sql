drop table if exists user_organizations;

create table user_organizations (
  id                      text primary key,
  user_id                 text not null references users,
  organization_id         text not null references organizations
);

comment on table user_organizations is '
  Each user is assigned a single organization to represent their own
  projects. This table records the org assigned to a user.
';

select audit.setup('public', 'user_organizations');
create index on user_organizations(user_id);
create index on user_organizations(organization_id);
create unique index user_organizations_user_id_not_del_idx on user_organizations(user_id) where deleted_at is null;
create unique index user_organizations_organization_id_not_del_idx on user_organizations(organization_id) where deleted_at is null;

