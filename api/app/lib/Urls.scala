package com.bryzek.dependency.api.lib

import io.flow.play.util.DefaultConfig

case class Urls(
  wwwHost: String = DefaultConfig.requiredString("dependency.www.host")
) {

  val github = "https://github.com/mbryzek/dependency"

  def www(rest: play.api.mvc.Call): String = {
    www(rest.toString)
  }

  def www(rest: String): String = {
    s"$wwwHost$rest"
  }

}

