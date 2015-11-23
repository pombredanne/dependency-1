update projects
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and name like 'Z Test %';

update libraries
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and guid not in (
     select library_versions.library_guid
       from library_versions
       join project_library_versions on project_library_versions.deleted_at is null and project_library_versions.library_version_guid = library_versions.guid
      where library_versions.deleted_at is null);

update library_versions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and library_guid not in (
     select libraries.guid
       from libraries
      where libraries.deleted_at is null
  );

update users
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and email like 'z-test-%';

