add organization_summary to resolver

UI:
  - Add page to see organizations, create organization, add members

  - From organization page, should see info on all its resolvers as
    well - or at least a link to the resolvers page filtered by the
    org

When saving dependencies, should we create a separate table for those
and track back to the file we found that dependency in? Right now,
since we create a library/binary directly, a typo in the build file of
a random project immediately will show as a version to upgrade
to. That is wrong. Also, if we persist information from the project
itself, we can actually record which file the dependency came from
which can be helpful.
