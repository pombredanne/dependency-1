package com.bryzek.dependency.lib

import io.flow.play.clients.UserTokenClient
import play.api.{Environment, Configuration, Mode}
import play.api.inject.Module

class DependencyClientProviderModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    env.mode match {
      case Mode.Prod | Mode.Dev => Seq(
        bind[DependencyClientProvider].to[DefaultDependencyClientProvider],
        bind[UserTokenClient].to[DefaultDependencyClientProvider]
      )
      case Mode.Test => Seq(
        // TODO: Add mock
        bind[DependencyClientProvider].to[DependencyClientProvider],
        bind[UserTokenClient].to[DefaultDependencyClientProvider]
      )
    }
  }

}
