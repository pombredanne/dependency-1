drop index tokens_user_id_tag_not_deleted_un_idx;
alter table tokens add description text;

alter table tokens add number_views bigint default 0 not null check (number_views >= 0);

comment on column tokens.number_views is '
  Controls retrieval of cleartext token - e.g. only can see the token once
';

