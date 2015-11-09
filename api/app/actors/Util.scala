package com.bryzek.dependency.actors

import play.api.Logger

/**
  * TODO: Extract to lib. Maybe lib-play-actors ??
  */
object Util {

  def withErrorHandler(
    description: String
  ) (
    f: => Any
  ) {
    try {
      f
    } catch {
      case t: Throwable => Logger.error(s"$description: ${t}" , t)
    }
  }

  def withVerboseErrorHandler(
    description: String
  ) (
    f: => Any
  ) {
    Logger.info(description)
    withErrorHandler(description)(f)
  }

}
