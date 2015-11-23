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

update languages
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and (lower(name) like 'z-test%' or lower(name) like 'z test%'); 

update language_versions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and language_guid not in (
     select languages.guid
       from languages
      where languages.deleted_at is null
  );

update users
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and email like 'z-test-%';

