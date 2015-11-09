dependency
==========
Tracks dependencies across multiple projects to manage upgrading of
dependencies. For example, if you are using a postgresql library, you
can register your project and start receiving immediate alerts for
upgrade events (like when a new version of that library is made
available).

Example query in maven:
  http://search.maven.org/solrsearch/select?q=g:%22com.google.inject%22+AND+a:%22guice%22&core=gav&rows=20&wt=json