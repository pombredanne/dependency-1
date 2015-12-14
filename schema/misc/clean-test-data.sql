update organizations
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and key like 'z-test%';

update projects
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and name like 'Z Test %';

update libraries
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and group_id like 'z-test%';

update library_versions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and library_guid not in (
     select libraries.guid
       from libraries
      where libraries.deleted_at is null
  );

update binaries
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and (lower(name) like 'z-test%' or lower(name) like 'z test%'); 

update binary_versions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and binary_guid not in (
     select binaries.guid
       from binaries
      where binaries.deleted_at is null
  );

update users
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and email like 'z-test-%';

update github_users
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and login like 'z-test-%';

update resolvers
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and uri like '%z-test.flow.io%';

update tokens
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and tag like 'z test%';

update items
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and label like 'z-test%';

update subscriptions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and user_guid in (select guid from users where deleted_at is not null);

update last_emails
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and user_guid in (select guid from users where deleted_at is not null);

