drop table if exists syncs;

create table syncs (
  id                       text primary key,
  type                       text not null check(util.lower_non_empty_trimmed_string(type)),
  object_id                text not null,
  event                      text not null check(util.lower_non_empty_trimmed_string(event))
);

comment on table syncs is '
  Records when we start and complete each sync of a module (e.g. project)
';

-- just capture creation. This table gets big and we delete from it
-- continuously
select schema_evolution_manager.create_basic_created_audit_data('public', 'syncs');
create index on syncs(type);
create index on syncs(object_id, event);

