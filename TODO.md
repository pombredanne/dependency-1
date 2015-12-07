 - Remove constant list of resolvers and stick in DB with visibility attribute (the global ones will be public).

 - For each library, record which resolver we found it on. This will
   enable libraries / binaries to be filterered for public/private
   based on the user