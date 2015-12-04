drop table if exists items;

create table items (
  guid                       uuid primary key,
  object_guid                uuid not null,
  label                      text not null check(non_empty_trimmed_string(label)),
  description                text check(trim(description) = description),
  detail                     json
);

comment on table items is '
  A denormalization of things that we want to search for. Basic model
  is that as the types are updated, we store a denormalized copy here
  just for search - e.g. projects, libraries, and binaries are
  denormalized here.
';

comment on column items.detail is '
  Detail specific to the type of object indexed. See the
  item_detail union type at http://apidoc.me/bryzek/dependency/latest
';

select schema_evolution_manager.create_basic_audit_data('public', 'items');
create unique index items_object_guid_not_deleted_un_idx on items(object_guid) where deleted_at is null;
