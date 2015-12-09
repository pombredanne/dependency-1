package com.bryzek.dependency.lib

import com.bryzek.dependency.v0.models.{Project, Resolver, Visibility}
import io.flow.user.v0.models.User

object Permissions {

  object Project {

    def edit(project: Project, user: Option[User]): Boolean = true
    def delete(project: Project, user: Option[User]): Boolean = edit(project, user)

  }

  object Resolver {

    def delete(resolver: Resolver, user: Option[User]): Boolean = {
      user.map(_.guid) == Some(resolver.user.guid)
    }

  }

}
