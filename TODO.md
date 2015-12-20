library / binary versions dao - findAll needs an Authorization

When adding a new resolver:
  - for all project_libraries for that org that have a null
    library_guid, attempt to resolve

UI:
  - Add page to see organizations, create organization, add members

  - From organization page, should see info on all its resolvers as
    well - or at least a link to the resolvers page filtered by the
    org

Handle http 500 from github
