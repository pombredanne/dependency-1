drop table if exists syncs;

create table syncs (
  guid                       uuid primary key,
  object_guid                uuid not null,
  event                      text not null check(enum(event))
);

comment on table syncs is '
  Records when we start and complete each sync of a module (e.g. project)
';

select schema_evolution_manager.create_basic_audit_data('public', 'syncs');
create index on syncs(object_guid, event);

