insert into resolvers
(guid, position, visibility, uri, created_by_guid, updated_by_guid)
select '272588ae-9b62-4cf1-bebd-2b55a82da482', 1, 'public', 'http://jcenter.bintray.com/',
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null),
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null);

insert into resolvers
(guid, position, visibility, uri, created_by_guid, updated_by_guid)
select 'eea634de-e888-40b6-916d-e600d883163a', 2, 'public', 'http://repo.typesafe.com/typesafe/ivy-releases/',
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null),
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null);

insert into resolvers
(guid, position, visibility, uri, created_by_guid, updated_by_guid)
select 'e64f96a3-633f-4d9d-a5cb-fa0d1b91d08a', 3, 'public', 'http://oss.sonatype.org/content/repositories/snapshots',
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null),
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null);

insert into resolvers
(guid, position, visibility, uri, created_by_guid, updated_by_guid)
select 'd5931164-7a17-446b-8b04-8b1b52c8b07e', 4, 'public', 'http://repo1.maven.org/maven2/',
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null),
       (select guid from users where email = 'system@bryzek.com' and deleted_at is null);
