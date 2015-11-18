package com.bryzek.dependency.lib

import play.api.{Environment, Configuration, Mode}
import play.api.inject.Module

import io.flow.play.clients.UserTokensClient

class UserTokensClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    Seq(
      bind[UserTokensClient].to[DefaultUserTokensClient]
    )
  }

}
