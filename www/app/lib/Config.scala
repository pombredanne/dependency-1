package com.bryzek.dependency.lib

import io.flow.play.util.DefaultConfig

object Config {
  lazy val githubClientId = DefaultConfig.requiredString("github.dependency.client.id")
}

