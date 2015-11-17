package com.bryzek.dependency.lib

import play.api.{Environment, Configuration, Mode}
import play.api.inject.Module

import io.flow.play.clients.UserTokenClient

class UserTokenClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    Seq(
      bind[UserTokenClient].to[DefaultUserTokenClient]
    )
  }

}
