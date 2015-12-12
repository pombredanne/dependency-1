package com.bryzek.dependency.api.lib

import io.flow.play.util.DefaultConfig

case class Urls(
  wwwHost: String = DefaultConfig.requiredString("dependency.www.host")
) {

  def www(rest: play.api.mvc.Call): String = {
    s"wwwHost$rest"
  }

  def www(rest: String): String = {
    s"wwwHost$rest"
  }

}

