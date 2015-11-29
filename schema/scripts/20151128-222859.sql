drop table if exists user_projects;
drop table if exists watch_projects;

create table watch_projects (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  project_guid            uuid not null references projects
);

comment on table watch_projects is '
  Stores the list of projects that a particular user is watching.
';

select schema_evolution_manager.create_basic_audit_data('public', 'watch_projects');
create index on watch_projects(user_guid);
create index on watch_projects(project_guid);
create unique index watch_projects_user_guid_project_guid_not_deleted_un_idx on watch_projects(user_guid, project_guid) where deleted_at is null;
