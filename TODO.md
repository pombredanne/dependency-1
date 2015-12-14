scope by org:
  - projects
     -- modify validation to include org
     -- add authorizatino to findAll

  - resolvers
     -- change user to organization
     -- add authorizatino to findAll

  - libraries
     -- add authorizatino to findAll

  - binaries
     -- add authorizatino to findAll

- Add Authorization object to restrict views of key objects to public
   / private records user can see
   - This is already implemented for the Resolvers class... consider if
     we want to build a SAAS groups model here or not.
 