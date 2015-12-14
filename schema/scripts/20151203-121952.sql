drop table if exists items;

create table items (
  guid                       uuid primary key,
  organization_guid          uuid not null references organizations,
  visibility                 text not null check(enum(visibility)),
  object_guid                uuid not null,
  label                      text not null check(non_empty_trimmed_string(label)),
  description                text check(trim(description) = description),
  summary                    json,
  contents                   text not null check(non_empty_trimmed_string(contents)) check(lower(contents) = contents)
);

comment on table items is '
  A denormalization of things that we want to search for. Basic model
  is that as the types are updated, we store a denormalized copy here
  just for search - e.g. projects, libraries, and binaries are
  denormalized here.
';

comment on column items.summary is '
  Information specific to the type of object indexed. See the
  item_detail union type at http://apidoc.me/bryzek/dependency/latest
';

comment on column items.contents is '
  All of the actual textual contents we search.
';

select schema_evolution_manager.create_basic_audit_data('public', 'items');
create unique index items_organization_guid_object_guid_not_deleted_un_idx on items(organization_guid, object_guid) where deleted_at is null;
create index on items(organization_guid);
